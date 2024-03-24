package com.corposense

import com.corposense.handlers.AccountHandler
import com.corposense.models.Account
import com.corposense.services.AccountService
import ratpack.exec.Promise
import ratpack.jackson.internal.DefaultJsonRender
import ratpack.thymeleaf3.Template
import spock.lang.Specification
import spock.lang.Unroll
import static ratpack.groovy.test.handling.GroovyRequestFixture.handle

class AccountSpec extends Specification {

    @Unroll
    def 'Should return list of all accounts'() {
        given:
        def accounts = []
        def promiseAccounts = Promise.value(accounts)
        def accountService = Mock(AccountService)
        accountService.getAll() >> promiseAccounts
        when:
        def result = handle(new AccountHandler(accountService)){
            uri ""
            method "get"
            header "Accept", "application/json"
            registry { it.add(AccountService, accountService) }
        }
        then:
        with (result){
            status.code == 200
//                rendered(DefaultJsonRender).object == accounts // In case rendering JSON
            rendered(Template).context.getVariable('servers') == accounts
            // Useless checks:
//                rendered(Template).name == 'server'
//                rendered(Template).context.variableNames[0] == 'servers'
        }
    }

    @Unroll
    def 'Should create an account'() {
        given:
        def account = new Account(
                name: 'Main Server',
                url: 'http://127.0.0.1:8080',
                username: 'admin',
                password: 'admin',
                active: true
        )
        def promiseAccount = Promise.value(account)
        def accountService = Mock(AccountService)
        accountService.get("1") >> promiseAccount
        when:
        def result = handle(new AccountHandler(accountService)){
            uri "1"
            method "GET"
            header "Accept", "application/json"
            registry { r ->
                r.add(AccountService, accountService)
            }
        }
        then:
        with (result){
            status.code == 200
            rendered(DefaultJsonRender).object == account
        }
    }

    @Unroll
    def 'Should delete an account'() {
        given:
        def accountService = Mock(AccountService)
        accountService.delete("1") >> Promise.ofNull()
        when:
        def result = handle(new AccountHandler(accountService)){
            uri "/delete"
            method "POST"
            header "Accept", "application/json"
        }
        then:
        result.status.code == 200
    }

}
