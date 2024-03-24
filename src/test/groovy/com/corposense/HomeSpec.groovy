package com.corposense

import com.corposense.handlers.AccountHandler
import com.corposense.models.Account
import com.corposense.services.AccountService
import com.corposense.services.DirectoriesService
import com.google.inject.Inject
import groovy.json.JsonOutput
import ratpack.exec.Promise
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.jackson.Jackson
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
    EmbeddedApp dms = GroovyEmbeddedApp.of {
     handlers {
         get('') {
             render 'Click <a href="/server">here to create a new Server account.'
         }
         get('directories') {
             render '{"directories" : [{"name" : "Administration", "files" : [{"id" : "123456789", "name" : "DOC001.pdf"}]}]}'
         }
     }
    }

    @Shared
    TestHttpClient testClient = app.httpClient

    @Unroll
    def 'Response list directories'() {
        given:
            def accountService = Mock(AccountService)

        when:
            def response = dms.httpClient.get('/directories')
            accountService.create(new Account(
                    name: 'Main Server',
                    url: "${dms.address}",
                    username: 'admin',
                    password: 'admin',
                    active: true
            )) >> Promise.value(1)

        then:
            response.statusCode == 200
            response.body.text == '{"directories" : [{"name" : "Administration", "files" : [{"id" : "123456789", "name" : "DOC001.pdf"}]}]}'
    }

/*    @Unroll
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
