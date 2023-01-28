package com.corposense.handlers

import com.corposense.models.Account
import com.corposense.services.AccountService
import com.corposense.services.ImageService
import com.corposense.services.UploadService
import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.func.Action
import ratpack.groovy.Groovy
import ratpack.handling.Chain

import static ratpack.jackson.Jackson.json
import static ratpack.jackson.Jackson.jsonNode
//import groovy.transform.CompileStatic

//@CompileStatic
class SaveEditedTextChain implements Action<Chain> {

    private final AccountService accountService
    private final UploadService uploadService
    private final ImageService imageService
    final Logger log = LoggerFactory.getLogger("ratpack.groovy")

    @Inject
    SaveEditedTextChain(AccountService accountService, UploadService uploadService, ImageService imageService){
        this.accountService = accountService
        this.uploadService = uploadService
        this.imageService = imageService
    }

    @Override
    void execute (Chain chain) throws Exception{
        Groovy.chain(chain){
            post('save') {
                render( parse(jsonNode()).map { JsonNode node ->
                    String editedText = new String(node.get('payload').asText().toString().decodeBase64())
                    String directoryId = node.get('directoryId').asText()
                    String languageId = node.get('languageId').asText()
                    String fileNameId = node.get('fileNameId').asText()
                    File outputDoc = imageService.htmlToPdf(new String(editedText),fileNameId)
                    File outputFile = imageService.renameFile(outputDoc.path,fileNameId)
                    accountService.getActive().then({ List<Account> accounts ->
                        Account account = accounts[0]
                        uploadService.uploadFile(outputFile,account.url, directoryId, languageId).then { Boolean result ->
                            if (result){
                                log.info("file: ${outputFile.name} has been uploaded.")
                            } else {
                                log.info("file cannot be uploaded.")
                            }
                        }
                    })
                    return json(['editedText': editedText ,
                                 'directoryId': directoryId ,
                                 'languageId': languageId ,
                                 'fileNameId':fileNameId])
                })
            }
        }

    }
}
