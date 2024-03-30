package com.corposense

import com.corposense.handlers.DmsRestEndpoint
import com.corposense.models.Account
import com.corposense.services.DirectoriesService
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.test.embed.EmbeddedApp
import spock.lang.Shared
import spock.lang.Specification
import static ratpack.groovy.test.handling.GroovyRequestFixture.handle

class DmsRestEndpointSpec extends Specification {

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

    def "will render directories"() {
        given:
            def directories = '{"folderId":"4","subFolders":[{"id":"100","name":"Administration","subFolders":[]}]}'

            Promise<String> listDirectoriesPromise = Promise.value(directories)
            def directoriesServices = Mock(DirectoriesService)
            def account = Mock(Account)
            directoriesServices.listDirectories(account, 4) >> listDirectoriesPromise
        when:
            def result = handle(new DmsRestEndpoint(directoriesServices, account)) { uri("api") }
        then:
            result.rendered(String) == directories
    }
}
