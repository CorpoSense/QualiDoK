package com.corposense.handlers

import com.corposense.models.Account
import com.corposense.services.AccountService
import com.corposense.services.DirectoriesService
import ratpack.func.Action
import ratpack.groovy.Groovy
import ratpack.handling.Chain
import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec

import javax.inject.Inject

import static ratpack.thymeleaf3.Template.thymeleafTemplate

class DmsRestEndpoint implements Action<Chain> {

    final int FOLDER_ID = 4
    private final DirectoriesService  directoriesService
    private final AccountService accountService

    @Inject
    DmsRestEndpoint(DirectoriesService directoriesService, AccountService accountService){
        this.directoriesService = directoriesService
        this.accountService = accountService
    }

    @Override
    void execute(Chain chain) throws Exception {
        Groovy.chain(chain) {
//            all {
            path('directories') {
                byMethod {
                    get {
                        def folderId = pathTokens["folderId"] ?: FOLDER_ID
                            accountService.getActive().then({ def accounts ->
                                Account account = accounts[0]
                                if (accounts.isEmpty() || !account){
                                    response.status(Status.NOT_FOUND).send('You must create a server account.')
                                } else {
                                    directoriesService.listDirectories(account, folderId).then({ def directories ->
                                        render(directories)
                                    })
                                }
                            })
                    }
                }
            } // api/directories
            
        }

    }
}
