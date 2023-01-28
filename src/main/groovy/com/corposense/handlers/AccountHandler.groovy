package com.corposense.handlers

import com.corposense.models.Account
import com.corposense.services.AccountService
import com.google.inject.Inject
import ratpack.form.Form
import ratpack.func.Action
import ratpack.groovy.Groovy
import ratpack.handling.Chain

import static ratpack.jackson.Jackson.json
import static ratpack.thymeleaf3.Template.thymeleafTemplate as view
//import groovy.transform.CompileStatic

//@CompileStatic
class AccountChain implements Action<Chain> {
    private final AccountService accountService

    @Inject
    AccountChain(AccountService accountService){
        this.accountService = accountService
    }

    @Override
    void execute(Chain chain) throws Exception {
        Groovy.chain(chain) {

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
        }//Groovy.chain

    }
}
