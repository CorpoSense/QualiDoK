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
             render 'Click <a href="/server">here to create a new Server account.</a>'
         }
         get('api') {
             render '{"folderId":[]}'
         }
         get('services/rest/folder/listChildren') {
             render '{"folderId":"4","subFolders":[{"id":"100","name":"Administration","subFolders":{"folderId":"100","subFolders":[{"id":"107","name":"Finance","subFolders":{"folderId":"107","subFolders":[]}}]}},{"id":"108","name":"Communication","subFolders":{"folderId":"108","subFolders":[{"id":"109","name":"External","subFolders":{"folderId":"109","subFolders":[]}},{"id":"110","name":"Internal","subFolders":{"folderId":"110","subFolders":[]}}]}},{"id":"102","name":"HR","subFolders":{"folderId":"102","subFolders":[{"id":"105","name":"IT","subFolders":{"folderId":"105","subFolders":[]}}]}}]}'
         }
     }
    }

    @Shared
    TestHttpClient testClient = app.httpClient

    @Unroll
    def 'Response list directories'() {
        when:
            def response = dms.httpClient.get('/services/rest/folder/listChildren')
        then:
            response.statusCode == 200
            response.body.text.startsWith('{"folderId":"4","subFolders":[{"id":"100"')
    }

    @Unroll
    def 'Should prompt user to create a server account'() {
        when:
        def response = dms.httpClient.get('/')
//        def response = testClient.get('/')
        then:
        response.body.text.contains('Click <a href="/server">here to create a new Server account.</a>')
    }

    @Unroll
    // TODO: Fix this test when decoupling the UI from the API
    def 'Should list directories'() {
        given:
        def directoriesService = Mock(DirectoriesService)
        def accountService = Mock(AccountService)
        accountService.create(new Account(
                name: 'Main Server',
                url: "${dms.address}",
                username: 'admin',
                password: 'admin',
                active: true
        )) >> Promise.value(1)
        def listPromise = Promise.value('{"folderId":[]}')
        directoriesService.listDirectories() >> listPromise
        when:
            def response = dms.httpClient.get('api')
        then:
            response.body.text.contains('"folderId"')
    }

/*    @Unroll
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
