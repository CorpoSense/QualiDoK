import com.corposense.ConnectionInitializer
import com.corposense.Constants
import com.corposense.H2ConnectionDataSource
import com.corposense.models.Account
import com.corposense.ratpack.Account.AccountChain
import com.corposense.ratpack.Ocr.OcrChain
import com.corposense.ratpack.Ocr.SaveEditedTextChain
import com.corposense.ratpack.Ocr.UploadDocChain
import com.corposense.services.AccountService
import com.zaxxer.hikari.HikariConfig

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ratpack.hikari.HikariModule
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.thymeleaf3.ThymeleafModule
import java.nio.file.Path
import static ratpack.groovy.Groovy.ratpack
import static ratpack.thymeleaf3.Template.thymeleafTemplate as view

import groovy.json.JsonSlurper

//import com.github.pemistahl.lingua.api.*
//import static com.github.pemistahl.lingua.api.Language.*

final Logger log = LoggerFactory.getLogger(ratpack)

final int FOLDER_ID = 4

/**
 * public/uploads path (use resolve() from relative path instead of absolutePath, the last one will be start from root disk)
 * It presents a full path on disk when running from Gradle, and relative path when running form a Jar file.
 */
String uploadDir = Constants.uploadDir
String publicDir = Constants.publicDir
String downloadsDir = Constants.downloadsDir
Path downloadPath = Constants.downloadPath
Path uploadPath = Constants.uploadPath

ratpack {
    serverConfig {
        development(true)
        port(3000)
        maxContentLength(26214400)  // 25Mb
    }
    bindings {
        module(ThymeleafModule)
        module( HikariModule, { HikariConfig config ->
            config.addDataSourceProperty("URL", "jdbc:h2:mem:account;INIT=CREATE SCHEMA IF NOT EXISTS DEV")
            config.dataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
        })
        bind(H2ConnectionDataSource)
        bind(AccountService)
        bindInstance(Service, new ConnectionInitializer())
        bind(OcrChain)
        bind(SaveEditedTextChain)
        bind(UploadDocChain)
        bind(AccountChain)

        add Service.startup('startup'){ StartEvent event ->
            if (serverConfig.development){
                sleep(500)
                event.registry.get(AccountService)
                        .create(new Account(
                                name: 'Main Server',
                                url: (System.getenv('GITPOD_HOST')?'http://127.0.0.1:8080':'http://0.0.0.0:8080/logicaldoc'),
                                username: 'admin',
                                password: 'admin',
                                active: true
                        )).then({ Integer id ->
                    log.info("Server N°: ${id} created.")
                })
            }

            [
                new File("${publicDir}/${uploadDir}"),
                new File("${publicDir}/${downloadsDir}")
            ].each { File baseUpload ->
                if (!baseUpload.exists()){
                    if (baseUpload.mkdirs()){
                        log.info("Created directory: ${baseUpload.absolutePath}")
                    } else {
                        log.error("Cannot create directory: ${baseUpload.absolutePath}")
                    }
                } else {
                    log.info("Directory: ${baseUpload} already exists.")
                }
            }

        }
    }
    handlers {

        get { AccountService accountService, HttpClient client  ->
            accountService.getActive().then({ List<Account> accounts ->
                Account account = accounts[0]
                if (accounts.isEmpty() || !account){
                    render(view("index", [message:'You must create a server account.']))
                } else {
                    // List of documents
                    def folderId = request.queryParams['folderId']?: FOLDER_ID
                    URI uri = "${account.url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()
                    client.get(uri){ RequestSpec reqSpec ->
                        reqSpec.basicAuth(account.username, account.password)
                        reqSpec.headers.set ("Accept", 'application/json')
                    }.then { ReceivedResponse res ->

                        JsonSlurper jsonSlurper = new JsonSlurper()
                        ArrayList directories = jsonSlurper.parseText(res.getBody().getText())

                        render(view('index',['directories' : directories , 'account': account ]))
                    }
                }
            })
        }

        get('preview'){
            render(view('preview'))
        }

        all(chain(registry.get(SaveEditedTextChain)))

        all(chain(registry.get(UploadDocChain)))

        get("${uploadPath}/:imagePath"){
            response.sendFile(new File("${uploadPath}","${pathTokens['imagePath']}").toPath())
        }

        get("${downloadPath}/:filePath"){
            response.sendFile(new File("${downloadPath}","${pathTokens['filePath']}").toPath())
        }

        prefix('upload') {
            all chain(registry.get(OcrChain))
        }

        prefix('server') {
            all chain(registry.get(AccountChain))
        }

        // Serve public files (assets...)
//        files { dir 'static' }
        files { dir publicDir }
    }
}
