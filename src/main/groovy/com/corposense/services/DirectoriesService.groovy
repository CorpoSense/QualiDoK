package com.corposense.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import groovy.json.JsonOutput
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

    Promise<String> listDirectories(String url, String username, String password, Serializable folderId) {
        URI uri = "${url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()

        Promise<ReceivedResponse> responsePromise = client.get(uri) { RequestSpec reqSpec ->
            reqSpec.basicAuth(username, password)
            reqSpec.headers.set("Accept", 'application/json')
        }

        Promise<String> directoriesPromise = responsePromise.map { ReceivedResponse res ->
            if (res.statusCode != 200) {
                throw new RuntimeException("Failed to retrieve directory list: ${res.body.text}")
            }

            JsonSlurper jsonSlurper = new JsonSlurper()
            String directories = JsonOutput.toJson(jsonSlurper.parseText(res.body.text) as List<Map<String, Object>>)
            directories
            //println(JsonOutput.prettyPrint(directories))

        } as Promise<String>

        directoriesPromise
    }

    Promise<ArrayList> listFolders(String url, String username, String password, Serializable folderId) {
        URI uri = "${url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()

        Promise<ReceivedResponse> responsePromise = client.get(uri) { RequestSpec reqSpec ->
            reqSpec.basicAuth(username, password)
            reqSpec.headers.set("Accept", 'application/json')
        }

        Promise<ArrayList> foldersPromise = responsePromise.map { ReceivedResponse res ->
            if (res.statusCode != 200) {
                throw new RuntimeException("Failed to retrieve directory list: ${res.body.text}")
            }

            JsonSlurper jsonSlurper = new JsonSlurper()
            ArrayList folders = jsonSlurper.parseText(res.body.text) as ArrayList

            folders
        }

        foldersPromise
    }
    Promise<String> listDocuments(String url, String username, String password, Serializable folderId) {
        URI uri = "${url}/services/rest/document/listDocuments?folderId=${folderId}".toURI()
        //URI uri = "${url}/services/rest/search/findByFilename?filename=Capture1.pdf".toURI()

        Promise<ReceivedResponse> responsePromise = client.get(uri) { RequestSpec reqSpec ->
            reqSpec.basicAuth(username, password)
            reqSpec.headers.set("Accept", 'application/json')
        }

        Promise<String> documentsPromise = responsePromise.map { ReceivedResponse res ->
            if (res.statusCode != 200) {
                throw new RuntimeException("Failed to retrieve directory list: ${res.body.text}")
            }

            JsonSlurper jsonSlurper = new JsonSlurper()
            String documents = JsonOutput.toJson(jsonSlurper.parseText(res.body.text) as List<Map<String, Object>>)
            documents
            println(JsonOutput.prettyPrint(documents))

        } as Promise<String>

        documentsPromise
    }

    Promise<ObjectNode> getFolderStructure(String url, String username, String password, Serializable folderId) {
        URI uri = "${url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()

        Promise<ReceivedResponse> responsePromise = client.get(uri, { reqSpec ->
            reqSpec.basicAuth(username, password)
            reqSpec.getHeaders().set("Accept", "application/json")
        })

        return responsePromise.flatMap({ res ->
            if (res.getStatusCode() != 200) {
                throw new RuntimeException("Failed to retrieve directory list: " + res.getBody().getText())
            }

            ObjectMapper objectMapper = new ObjectMapper()
            ObjectNode root = JsonNodeFactory.instance.objectNode()
            ArrayNode subFolders = JsonNodeFactory.instance.arrayNode()

            List<Map<String, Object>> directoryList = objectMapper.readValue(res.getBody().getText(), List.class)

            for (Map<String, Object> directory : (directoryList as List<Map<String, Object>>)) {
                String id = directory.get("id").toString()
                String name = directory.get("name").toString()

                ObjectNode subFolder = JsonNodeFactory.instance.objectNode()
                subFolder.put("id", id)
                subFolder.put("name", name)

                Promise<ObjectNode> subFolderPromise = getFolderStructure(url, username, password, id)
                subFolderPromise.then({ subFolderStructure -> subFolder.set("subFolders", subFolderStructure) })
                subFolders.add(subFolder)
            }

            root.put("folderId", folderId.toString())
            root.replace("subFolders", subFolders)

            return Promise.value(root)
        })
    }

}
