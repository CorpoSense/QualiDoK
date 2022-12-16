package com.corposense.ratpack.Ocr

import com.corposense.Constants
import com.corposense.models.Account
import com.corposense.services.AccountService
import com.corposense.services.ImageService
import com.corposense.services.UploadService
import com.google.inject.Inject
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.form.Form
import ratpack.form.UploadedFile
import ratpack.func.Action
import ratpack.groovy.Groovy
import ratpack.handling.Chain
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec

import java.nio.file.Path

import static ratpack.thymeleaf3.Template.thymeleafTemplate as view


class OcrChain implements Action<Chain> {

    private final HttpClient client
    private final AccountService accountService
    private final UploadService uploadService
    private final ImageService imageService

    @Inject
    OcrChain(HttpClient client, AccountService accountService, UploadService uploadService, ImageService imageService){
        this.client = client
        this.accountService = accountService
        this.uploadService = uploadService
        this.imageService = imageService
    }

    @Override
    void execute(Chain chain) throws Exception {
        Path uploadPath = Constants.uploadPath
        final Logger log = LoggerFactory.getLogger("ratpack.groovy")
        final int FOLDER_ID = 4
        final String[] SUPPORTED_DOCS = ['pdf', 'doc', 'docx']
        final String[] SUPPORTED_IMAGES = ['png', 'jpg', 'jpeg']
        final String[] SUPPORTED_FILES = SUPPORTED_IMAGES + SUPPORTED_DOCS

            Groovy.chain(chain) {
                path{
                    byMethod{
                        get {
                            render(view('upload'))
                        }
                        post {
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

                                            switch (typeOcr) {
                                                case 'extract-text':
                                                    File inputFile = new File("${uploadPath}", uploadedFile.fileName)
                                                    uploadedFile.writeTo(inputFile.newOutputStream())
                                                    String fileName = imageService.getFileNameWithoutExt(inputFile)
                                                    log.info("File type: ${fileType}")
                                                    // TODO: support doc, docx document
//                                        if (SUPPORTED_DOCS.any {fileType.contains(it)}){...}
                                                    if (fileType.contains('pdf')) {
                                                        // Handle PDF document...
                                                        List<String> fullText = imageService.produceTextForMultipleImg(inputFile)
                                                        // List of directories
                                                        def folderId = request.queryParams['folderId'] ?: FOLDER_ID
                                                        URI uri = "${account.url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()
                                                        client.get(uri) { RequestSpec reqSpec ->
                                                            reqSpec.basicAuth(account.username, account.password)
                                                            reqSpec.headers.set("Accept", 'application/json')
                                                        }.then { ReceivedResponse res ->

                                                            JsonSlurper jsonSlurper = new JsonSlurper()
                                                            ArrayList directories = jsonSlurper.parseText(res.getBody().getText())

                                                            render(view('preview', [
                                                                    'message'     : (fullText ? 'Image processed successfully.' : 'No output can be found.'),
                                                                    'inputPdfFile': inputFile.path,
                                                                    'fileName'    : fileName,
                                                                    'fullText'    : fullText,
                                                                    'directories' : directories
                                                                    //'detectedLanguage': detectedLanguage
                                                            ]))
                                                        }
                                                    } else if (SUPPORTED_IMAGES.any { fileType.contains(it) }) {
                                                        // Handle image document
                                                        String fullText = imageService.produceText(inputFile)
//                                                    LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(ENGLISH, ARABIC, FRENCH, GERMAN, SPANISH).build()
//                                                    Language detectedLanguage = detector.detectLanguageOf(fullText)
//                                                    def confidenceValues = detector.computeLanguageConfidenceValues(text: "Coding is fun.")
//                                                    log.info("detectedLanguage: ${detectedLanguage}")


                                                        // List of directories
                                                        def folderId = request.queryParams['folderId'] ?: FOLDER_ID
                                                        URI uri = "${account.url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()
                                                        client.get(uri) { RequestSpec reqSpec ->
                                                            reqSpec.basicAuth(account.username, account.password)
                                                            reqSpec.headers.set("Accept", 'application/json')
                                                        }.then { ReceivedResponse res ->

                                                            JsonSlurper jsonSlurper = new JsonSlurper()
                                                            ArrayList directories = jsonSlurper.parseText(res.getBody().getText())

                                                            render(view('preview', [
                                                                    'message'    : (fullText ? 'Image processed successfully.' : 'No output can be found.'),
                                                                    'inputImage' : inputFile.path,
                                                                    'fileName'   : fileName,
                                                                    'fullText'   : fullText,
                                                                    'directories': directories
//                                                           'detectedLanguage': detectedLanguage
                                                            ]))
                                                        }

                                                    } else {
                                                        // Handle other type of documents
                                                        render(view('preview', ['message': 'This file type is not currently supported.']))
                                                    }
                                                    break
                                                case 'produce-pdf':
                                                    File inputFile = new File("${uploadPath}", uploadedFile.fileName)
                                                    uploadedFile.writeTo(inputFile.newOutputStream())
                                                    String fileName = imageService.getFileNameWithoutExt(inputFile)
                                                    log.info("File type: ${fileType}")
                                                    // TODO: support doc, docx document
//                                        if (SUPPORTED_DOCS.any {fileType.contains(it)}){...}
                                                    if (fileType.contains('pdf')) {
                                                        // Handle PDF document...
                                                        File outputFile = imageService.producePdfForMultipleImg(inputFile)
                                                        // List of directories
                                                        def folderId = request.queryParams['folderId'] ?: FOLDER_ID
                                                        URI uri = "${account.url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()
                                                        client.get(uri) { RequestSpec reqSpec ->
                                                            reqSpec.basicAuth(account.username, account.password)
                                                            reqSpec.headers.set("Accept", 'application/json')
                                                        }.then { ReceivedResponse res ->
                                                            JsonSlurper jsonSlurper = new JsonSlurper()
                                                            ArrayList directories = jsonSlurper.parseText(res.getBody().getText())

                                                            render(view('preview', [
                                                                    'message'     : ('Document generated successfully.'),
                                                                    'inputPdfFile': inputFile.path,
                                                                    'fileName'    : fileName,
                                                                    'outputFile'  : outputFile,
                                                                    'directories' : directories
                                                                    //'detectedLanguage': detectedLanguage
                                                            ]))
                                                        }

                                                    } else if (SUPPORTED_IMAGES.any { fileType.contains(it) }) {
                                                        // Handle image document (TODO: make visibleImageLayer dynamic)
                                                        File outputFile = imageService.producePdf(inputFile, 0)

                                                        // List of directories
                                                        def folderId = request.queryParams['folderId'] ?: FOLDER_ID
                                                        URI uri = "${account.url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()
                                                        client.get(uri) { RequestSpec reqSpec ->
                                                            reqSpec.basicAuth(account.username, account.password)
                                                            reqSpec.headers.set("Accept", 'application/json')
                                                        }.then { ReceivedResponse res ->
                                                            JsonSlurper jsonSlurper = new JsonSlurper()
                                                            ArrayList directories = jsonSlurper.parseText(res.getBody().getText())

                                                            render(view('preview', [
                                                                    'message'    : 'Document generated successfully.',
                                                                    'inputImage' : inputFile.path,
                                                                    'fileName'   : fileName,
                                                                    'outputFile' : outputFile.path,
                                                                    'directories': directories
                                                            ]))
                                                        }
                                                    } else {
                                                        // Handle other type of documents
                                                        render(view('preview', ['message': 'This file type is not currently supported.']))
                                                    }
                                                    break
                                                default:
                                                    render('Error: Invalid option value.')
                                                    return
                                            }
                                        } else {
                                            render(view('preview', ['message': "${files.size()} document(s)"]))
                                        }
                                    }
                                }
                            })
                        }
                    }
                }

            }
        }
    }

