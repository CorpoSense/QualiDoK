package com.corposense

import com.corposense.handlers.AccountHandler
import com.corposense.models.Account
import com.corposense.services.AccountService
import com.corposense.services.DirectoriesService
import com.google.inject.Inject
import ratpack.exec.Promise
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.jackson.internal.DefaultJsonRender
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import ratpack.thymeleaf3.Template
import spock.lang.AutoCleanup
import spock.lang.IgnoreRest
import spock.lang.MockingApi
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import static ratpack.groovy.test.handling.GroovyRequestFixture.handle
import static ratpack.thymeleaf3.Template.thymeleafTemplate

/**
 * HomePage Tests (./gradlew test --tests HomeSpec)
 */
class HomeSpec extends Specification {

    @AutoCleanup
    @Shared
    GroovyRatpackMainApplicationUnderTest app = new GroovyRatpackMainApplicationUnderTest()

    @Shared
    EmbeddedApp mockDms = GroovyEmbeddedApp.of {
     handlers {
         all {
             render '{"directories" : [{"name" : "Administration", "files" : [{"id" : "123456789", "name" : "DOC001.pdf"}]}]}'
         }
     }
    }

    @Shared
    TestHttpClient testClient = app.httpClient

    @Unroll
    def 'Should create a client api'() {
        expect:
            testClient != null
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
                method "get"
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
    @IgnoreRest
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
                registry { r ->
                    r.add(AccountService, accountService)
                }

            }
        then:
            with (result){
                status.code == 200
//                rendered(DefaultJsonRender).object == accounts // In case rendering JSON
                rendered(Template).name == 'server'
                rendered(Template).context.variableNames[0] == 'servers'
                rendered(Template).context.getVariable('servers') == []
            }
    }

/*
// In order to run these tests the server must be up and running
    @Unroll
    def 'Response should hit the endpoint'() {
        when:
        def response = mockDms.httpClient.get('/')

        then:
        response.statusCode == 200
        response.body.text == '{"directories" : [{"name" : "Administration", "files" : [{"id" : "123456789", "name" : "DOC001.pdf"}]}]}'
    }

    @Unroll
    def 'Should prompt user to create a server account'() {
        when:
        def response = testClient.get('/')
        then:
        response.body.text.contains('Click <a href="/server">here to create a new Server account.')
    }

    @Unroll
    def 'Should list directories'() {
        when:
        def response = testClient.get('/')
        then:
        response.body.text.contains('List of Directories')
    }

    @Unroll
    def 'Should preview a document'() {
        when:
        def response = testClient.get('/preview')
        then:
        response.body.text.contains('Preview')
    }

    @Unroll
    def 'Should implements CRUD operations'() {
        given:
            def accountService = Mock(AccountService)
            def account = Mock(Account)
        when:
            testClient.get('/')
        then:
            accountService.create(account) == null
            accountService.getAll() == null
            accountService.get('admin') == null
            accountService.delete('admin') == null
            accountService.getActive() == null
    }

    @Unroll
//    @IgnoreRest
    def 'Should list directories from the server'(){
        given:
            def directoriesService = Mock(DirectoriesService)
            def accountService = Mock(AccountService)
            def account = Mock(Account)
            def folderId = 4
            def client = Mock(ratpack.http.client.HttpClient)
        when:
            testClient.get('/')
        then:
            0 * directoriesService.listDirectories()
    }
*/
}
