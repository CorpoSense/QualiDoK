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

class UploadOfficeChain implements Action<Chain> {
    private final HttpClient client
    private final AccountService accountService
    private final ImageService imageService
    final int FOLDER_ID = 4
    @Inject
    UploadOfficeChain(HttpClient client, AccountService accountService, ImageService imageService){
        this.client = client
        this.accountService = accountService
        this.imageService = imageService
    }
    @Override
    void execute(Chain chain) throws Exception {
        Path uploadPath = Constants.uploadPath
        final Logger log = LoggerFactory.getLogger("ratpack.groovy")
        Groovy.chain(chain) {
            path {
                byMethod {
                    get {
                        render(view('upload'))
                    }
                    post {
                        accountService.getActive().then({ List<Account> accounts ->
                            Account account = accounts[0]
                            if (accounts.isEmpty() || !account) {
                                render(view('upload', [message: 'You must create a server account.']))
                            } else {
                                parse(Form).then { Form form ->
                                    List<UploadedFile> files = form.files('input-docx')
                                    // Single document upload
                                    UploadedFile uploadedFile = files.first()
                                    //String fileType = uploadedFile.contentType.type
                                    File inputFile = new File("${uploadPath}", uploadedFile.fileName)
                                    uploadedFile.writeTo(inputFile.newOutputStream())
                                    String fileName = imageService.getFileNameWithoutExt(inputFile)
                                    def folderId = request.queryParams['folderId'] ?: FOLDER_ID
                                    URI uri = "${account.url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()
                                    client.get(uri) { RequestSpec reqSpec ->
                                        reqSpec.basicAuth(account.username, account.password)
                                        reqSpec.headers.set("Accept", 'application/json')
                                    }.then { ReceivedResponse res ->

                                        JsonSlurper jsonSlurper = new JsonSlurper()
                                        ArrayList directories = jsonSlurper.parseText(res.getBody().getText())

                                        render(view('previewOffice', [
                                                'outputFile': inputFile.path,
                                                'fileName'     : fileName,
                                                'directories'  : directories
                                        ]))
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
