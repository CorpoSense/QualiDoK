package com.corposense.handlers

import com.corposense.models.Account
import com.corposense.services.AccountService
import com.corposense.services.DirectoriesService
import ratpack.func.Action
import ratpack.groovy.Groovy
import ratpack.handling.Chain

import javax.inject.Inject

class DmsRestEndpoint implements Action<Chain> {

    final int FOLDER_ID = 4
    private final DirectoriesService  directoriesService
    private final Account  account
//    private final AccountService accountService

    @Inject
    DmsRestEndpoint(DirectoriesService directoriesService, Account account /*, AccountService accountService*/){
        this.directoriesService = directoriesService
//        this.accountService = accountService
        this.account = account
    }

    @Override
    void execute(Chain chain) throws Exception {
        Groovy.chain(chain) {
            path('api') {
                byMethod {
                    get {
                        def folderId = pathTokens["folderId"] ?: FOLDER_ID
//                        accountService.get(accountId).then({ def account ->
                            directoriesService.listDirectories(account, folderId).then({ def directories ->
                                render(directories)
                            })
//                        })
                    }
                }
            }
        }

    }
}
