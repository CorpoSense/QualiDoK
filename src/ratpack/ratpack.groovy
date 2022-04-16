import com.corposense.ConnectionInitializer
import com.corposense.H2ConnectionDataSource
import com.corposense.models.Account
import com.corposense.ocr.ImageConverter
import com.corposense.services.AccountService
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
import ratpack.server.BaseDir
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.thymeleaf3.ThymeleafModule
import java.nio.file.Path
import static ratpack.groovy.Groovy.ratpack
import static ratpack.thymeleaf3.Template.thymeleafTemplate as view
import static ratpack.jackson.Jackson.json
import static ratpack.jackson.Jackson.fromJson
import com.github.pemistahl.lingua.api.*
import static com.github.pemistahl.lingua.api.Language.*

final Logger log = LoggerFactory.getLogger(ratpack)

final int FOLDER_ID = 4
final String[] SUPPORTED_DOCS = ['pdf', 'doc', 'docx']
final String[] SUPPORTED_IMAGES = ['png', 'jpg', 'jpeg']
final String[] SUPPORTED_FILES = SUPPORTED_IMAGES + SUPPORTED_DOCS

def uploadDir = 'uploads'
def publicDir = 'public'
Path baseDir = BaseDir.find("${publicDir}/${uploadDir}")
Path uploadPath = baseDir.resolve(uploadDir)

String generatedFilesDir = "generatedFiles"
String createdFilesDir = "createdFiles"
Path baseGeneratedFilesDir = BaseDir.find("${publicDir}/${generatedFilesDir}")
Path baseCreatedFilesDir = BaseDir.find("${publicDir}/${generatedFilesDir}/${createdFilesDir}")

Path generatedFilesPath = baseGeneratedFilesDir.resolve(generatedFilesDir)
Path createdFilesPath = baseCreatedFilesDir.resolve(createdFilesDir)

ratpack {
    serverConfig {
        development(true)
        port(3000)
        maxContentLength(26214400)
    }
    bindings {
        module (ThymeleafModule)
        module ( HikariModule, { HikariConfig config ->
            config.addDataSourceProperty("URL", "jdbc:h2:mem:account;INIT=CREATE SCHEMA IF NOT EXISTS DEV")
            config.dataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
        })
        bind (H2ConnectionDataSource)
        bind (AccountService)
        bind (UploadService)
        bind(ImageConverter)
        bindInstance (Service, new ConnectionInitializer())

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

            new File("${publicDir}/${uploadDir}").with { File baseUpload ->
                if (!baseUpload.exists()){
                    if (baseUpload.mkdirs()){
                        log.info("Created directory: ${baseUpload.absolutePath}")
                    }
                }
            }

        }
    }
    handlers {

        get { AccountService accountService, ImageConverter imageConverter ->
            accountService.getActive().then({ List<Account> accounts ->
                Account account = accounts[0]
                if (accounts.isEmpty() || !account){
                    render(view("index", [message:'You must create a server account.']))
                } else {
                    render(view("index", ['account': account]))
                }
            })
        }

        get('list') { HttpClient client, AccountService accountService ->
            accountService.getActive().then({ List<Account> accounts ->
                Account account = accounts[0]
                if (accounts.isEmpty() || !account){
                    render(json([:]))
                } else {
                    // List of documents
                    def folderId = request.queryParams['folderId']?:FOLDER_ID
                    URI url = "${account.url}/logicaldoc/services/rest/folder/listChildren?folderId=${folderId}".toURI()
                    client.get(url) { RequestSpec reqSpec ->
                        reqSpec.basicAuth(account.username, account.password)
                        reqSpec.headers.set ("Accept", 'application/json')
                    }.then { ReceivedResponse res ->
                        res.forwardTo(response)
                        // def directories = new groovy.json.JsonSlurper(res.body.text)
                        // render(view('list', [directories: directories]))
                    }
                }

            })
        } // list

        get('preview'){
            render(view('preview'))
        }

        prefix('upload') {
            // path('/pdf'){}
            all { AccountService accountService ->
                byMethod {
                    get {
                        render(view('upload'))
                    }
                    post { UploadService uploadService, ImageConverter imageConverter ->
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
                                                    String fullText = imageConverter.produceText(inputFile.path)
                                                    LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(ENGLISH, ARABIC, FRENCH, GERMAN, SPANISH).build()
                                                    Language detectedLanguage = detector.detectLanguageOf(fullText)
//                def confidenceValues = detector.computeLanguageConfidenceValues(text: "Coding is fun.")
                                                    log.info("detectedLanguage: ${detectedLanguage}")

                                                    render(view('preview', [
                                                            'message':'Image processed successfully.',
                                                            'inputImage': inputFile.name,
                                                            'fullText': fullText,
                                                            'detectedLanguage': detectedLanguage
                                                    ]))
                                                } else {
                                                    // Handle other type of documents
                                                    render(view('preview', ['message':'This file type is not currently supported.']))
                                                }


                                                break;
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
                                                    // Handle image document
                                                    String outputFile = imageConverter.produceTextOnlyPdf(inputFile.path, 0)
                                                    if (new File(outputFile).exists()){
                                                        log.info("Found at: ${outputFile}")
                                                    } else {
                                                        log.info("CANNOT FIND file: ${outputFile}")
                                                    }
                                                    render(view('preview', [
                                                            'message':'Document generated successfully.',
                                                            'inputImage': inputFile.name,
                                                            'outputFile': outputFile
                                                    ]))
                                                } else {
                                                    // Handle other type of documents
                                                    render(view('preview', ['message':'This file type is not currently supported.']))
                                                }


                                                break;
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
        }


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
        }

        // Serve assets files
        files { dir "public" }
    }
}
