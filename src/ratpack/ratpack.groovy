import com.corposense.ConnectionInitializer
import com.corposense.Constants
import com.corposense.H2ConnectionDataSource
import com.corposense.models.Account
import com.corposense.ocr.ImageConverter
import com.corposense.services.AccountService
import com.corposense.services.ImageService
import com.corposense.services.UploadService
import com.zaxxer.hikari.HikariConfig

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ratpack.form.Form
import ratpack.form.UploadedFile
import ratpack.hikari.HikariModule
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.jackson.Jackson
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.thymeleaf3.ThymeleafModule
import java.nio.file.Path
import static ratpack.groovy.Groovy.ratpack
import static ratpack.jackson.Jackson.jsonNode
import static ratpack.thymeleaf3.Template.thymeleafTemplate as view
import static ratpack.jackson.Jackson.json

import groovy.json.JsonSlurper

//import com.github.pemistahl.lingua.api.*
//import static com.github.pemistahl.lingua.api.Language.*

final Logger log = LoggerFactory.getLogger(ratpack)

final int FOLDER_ID = 4
final String[] SUPPORTED_DOCS = ['pdf', 'doc', 'docx']
final String[] SUPPORTED_IMAGES = ['png', 'jpg', 'jpeg']
final String[] SUPPORTED_FILES = SUPPORTED_IMAGES + SUPPORTED_DOCS

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
        bind(UploadService)
        bind(ImageConverter)
        bind(ImageService)
        bindInstance(Service, new ConnectionInitializer())

        add Service.startup('startup'){ StartEvent event ->
            if (serverConfig.development){
                sleep(500)
                event.registry.get(AccountService)
                        .create(new Account(
                                name: 'Main Server',
                                url: 'http://0.0.0.0:8080',
                                // url: 'http://127.0.0.1:8080',
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

        get { AccountService accountService /*, ImageService imageService */, HttpClient client  ->
/*
            String outputText = ''
            File outputFile = null
            File inputImage = new File("${uploadPath}",'text-tiny.jpg')
            if (inputImage.exists()){
                log.info("File: ${inputImage} exists, converting...")
//                outputText = imageService.produceText(inputImage)
//                log.info("OutputText: ${outputText}")
                outputFile = imageService.producePdf(inputImage)
            } else {
                log.info("File ${inputImage} does not exists.")
            }
            if (outputFile.exists()){
                render "File at: ${outputFile.path}"
                return
            }
            render "Cannot find file at: ${outputFile.path}"
*/

/*            render """
            baseDir: ${baseDir}, exists: ${new File(baseDir.toString()).exists()}
            uploadPath: ${uploadPath}, exists: ${new File(uploadPath.toString()).exists()}
            uploadPath: ${downloadsPath}, exists: ${new File(downloadsPath.toString()).exists()}
            """
            */
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
        post('save'){
           // ImageService imageService ->
            render( parse(jsonNode()).map { def node ->
                def editedText = node.get('payload').asText()
                def imagePath = node.get('inputImage').asText()
                log.info(editedText)
                log.info(imagePath)
                Jackson.json(['message1': editedText , 'message2':imagePath])

            })
            //TODO: create pdf file that contain the editedText

            // imageService.generateDocument(editedText,imagePath)
        }



        get("${uploadPath}/:imagePath"){
            response.sendFile(new File("${uploadPath}","${pathTokens['imagePath']}").toPath())
        }

        get("${downloadPath}/:filePath"){
            response.sendFile(new File("${downloadPath}","${pathTokens['filePath']}").toPath())
        }

        prefix('upload') {
            // path('/pdf'){}
            all { AccountService accountService ->
                byMethod {
                    get {
                        HttpClient client ->
                            accountService.getActive().then({ List<Account> accounts ->
                                Account account = accounts[0]

                                // List of directories
                                def folderId = request.queryParams['folderId']?: FOLDER_ID
                                URI uri = "${account.url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()
                                client.get(uri){ RequestSpec reqSpec ->
                                    reqSpec.basicAuth(account.username, account.password)
                                    reqSpec.headers.set ("Accept", 'application/json')
                                }.then { ReceivedResponse res ->

                                    JsonSlurper jsonSlurper = new JsonSlurper()
                                    ArrayList directories = jsonSlurper.parseText(res.getBody().getText())
                                    render(view('upload',['directories' : directories ]))

                                }
                            })
                        //render(view('upload'))
                    }
                    post { UploadService uploadService, ImageService imageService ->
                        accountService.getActive().then({ List<Account> accounts ->
                            Account account = accounts[0]
                            if (accounts.isEmpty() || !account){
                                render(view('upload', [message:'You must create a server account.']))
                            } else {
                                parse(Form).then { Form form ->
                                    List<UploadedFile> files = form.files('input-doc')
                                    log.info("Detected: ${files.size()} document(s).")

                                    if (files.size() == 0){
                                        render(view('preview', ['message': "No file uploaded!"]))

                                    } else if (files.size() == 1){
                                        // Single document upload
                                        UploadedFile uploadedFile = files.first()
                                        String fileType = uploadedFile.contentType.type

                                        if (!SUPPORTED_FILES.any { fileType.contains(it)} ){
                                            // TODO: may need to back to /upload page
                                            render(view('preview', ['message':'This type of file is not supported.']))
                                            return
                                        }
                                        String typeOcr = form.get('type-ocr')
                                        log.info("Type of processing: ${typeOcr}")
                                        
                                        //TODO: ISSUE-> How to get the selected folder from /upload
                                        //returns:The directory choosed is: ${directory.id}
                                        String directoryId = form.get('folderId')
                                        log.info("The directory choosed is: ${directoryId}")

                                        switch (typeOcr){
                                            case 'extract-text':
                                                File inputFile = new File("${uploadPath}", uploadedFile.fileName)
                                                uploadedFile.writeTo(inputFile.newOutputStream())
                                                log.info("File type: ${fileType}")
                                                // TODO: support doc, docx document
//                                        if (SUPPORTED_DOCS.any {fileType.contains(it)}){...}
                                                if (fileType.contains('pdf')){
                                                    // Handle PDF document...
                                                    render(view('preview', ['message':'This is a PDF document']))
                                                } else if (SUPPORTED_IMAGES.any {fileType.contains(it)}){
                                                    // Handle image document
                                                    String fullText = imageService.produceText(inputFile)
//                                                    LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(ENGLISH, ARABIC, FRENCH, GERMAN, SPANISH).build()
//                                                    Language detectedLanguage = detector.detectLanguageOf(fullText)
//                                                    def confidenceValues = detector.computeLanguageConfidenceValues(text: "Coding is fun.")
//                                                    log.info("detectedLanguage: ${detectedLanguage}")

                                                    render(view('preview', [
                                                            'message': (fullText? 'Image processed successfully.':'No output can be found.'),
                                                            'inputImage': inputFile.path,
                                                            'fullText': fullText,
//                                                            'detectedLanguage': detectedLanguage
                                                    ]))
                                                    imageService.generateDocument(fullText,inputFile.toString())

                                                } else {
                                                    // Handle other type of documents
                                                    render(view('preview', ['message':'This file type is not currently supported.']))
                                                }
                                                break
                                            case 'produce-pdf':

                                                File inputFile = new File("${uploadPath}", uploadedFile.fileName)
                                                uploadedFile.writeTo(inputFile.newOutputStream())
                                                log.info("File type: ${fileType}")
                                                // TODO: support doc, docx document
//                                        if (SUPPORTED_DOCS.any {fileType.contains(it)}){...}
                                                if (fileType.contains('pdf')){
                                                    // Handle PDF document...
                                                    render(view('preview', ['message':'This is a PDF document']))
                                                } else if (SUPPORTED_IMAGES.any {fileType.contains(it)}){
                                                    // Handle image document (TODO: make visibleImageLayer dynamic)
                                                    File outputFile = imageService.producePdf(inputFile, 0)
                                                    //Upload pdf document to LogicalDoc

                                                    uploadService.uploadFile(outputFile, account.url, 4, 'eng').then { Boolean result ->
                                                        if (result){
                                                            log.info("file: ${outputFile.name} has been uploaded.")
                                                        } else {
                                                            log.info("file cannot be uploaded.")
                                                        }

                                                    }
                                                    render(view('preview', [
                                                            'message':'Document generated successfully.',
                                                            'inputImage': inputFile.path,
                                                            'outputFile': outputFile.path
                                                    ]))
                                                } else {
                                                    // Handle other type of documents
                                                    render(view('preview', ['message':'This file type is not currently supported.']))
                                                }


                                                break
                                            default:
                                                render('Error: Invalid option value.')
                                                return
                                        }


                                    } else {
                                        render(view('preview', ['message': "${files.size()} document(s)"]))
                                    }
                                    /*
                                    files.each { UploadedFile uploadedFile ->
                                        if (uploadedFile.contentType.type.contains('pdf')){
                                            log.info("${uploadedFile.fileName} (${uploadedFile.bytes.size()})")
                                            File outputFile = new File("${uploadPath}", uploadedFile.fileName)
                                            uploadedFile.writeTo(outputFile.newOutputStream())
                                            // TODO: we'll make the language dynamically detected
//                                            TODO:
//                                                1- Upload a document via the browser
//                                                2- Check using the preview if the result of OCR is satisfied
//                                                3- if it's ok then upload to LogicalDOC.
                                            uploadService.uploadFile(outputFile, account.url, 4, 'fr').then { Boolean result ->
                                                if (result){
                                                    log.info("file: ${outputFile.name} has been uploaded.")
                                                } else {
                                                    log.info("file cannot be uploaded.")
                                                }
                                            }
                                        }
                                    } // each()
                                    render "uploaded: ${files.size()} file(s)"
                                    */
                                }
                            }
                        })

                    }
                }
            }
        } //prefix: '/upload'

        prefix('server') {

            path("delete") { AccountService accountService ->
                byMethod {
                    post {
                        parse(Form).then { Form map ->
                            accountService.delete(map['id']).then { Integer id ->
                                redirect('/server')
                            }
                        }
                    }
                }
            }

            path(':id'){ AccountService accountService ->
                byMethod {
                    get {
                        accountService.get(pathTokens['id']).then { Account account ->
                            render(json(account))
                        }
                    }
                }
            }

            all { AccountService accountService ->
                byMethod {
                    get {
                        accountService.all.then { List<Account> accounts ->
                            render(view('server', [servers: accounts]))
                        }
                    }
                    post {
                        parse(Form).then { Form map ->
                            accountService.create( new Account(map) ).then { Integer id ->
                                redirect('/server')
                            }
                        }
                    }
                } // byMethod
            } // all
        } // prefix('/server')

        // Serve public files (assets...)
//        files { dir 'static' }
        files { dir publicDir }
    }
}
