package com.corposense.services
import groovy.json.JsonSlurper
import ratpack.exec.Promise
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
class DirectoriesService {

    private final HttpClient client

    DirectoriesService(HttpClient client) {
        this.client = client
    }

    Promise<ArrayList> listDirectories(String url, String username, String password, Serializable folderId) {
        URI uri = "${url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()

        Promise<ReceivedResponse> responsePromise = client.get(uri) { RequestSpec reqSpec ->
            reqSpec.basicAuth(username, password)
            reqSpec.headers.set("Accept", 'application/json')
        }

        Promise<ArrayList> directoriesPromise = responsePromise.map { ReceivedResponse res ->
            if (res.statusCode != 200) {
                throw new RuntimeException("Failed to retrieve directory list: ${res.body.text}")
            }

            JsonSlurper jsonSlurper = new JsonSlurper()
            ArrayList directories = jsonSlurper.parseText(res.body.text) as ArrayList

            directories
        }

        directoriesPromise
    }


}
