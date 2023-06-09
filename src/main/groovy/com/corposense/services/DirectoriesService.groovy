package com.corposense.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.inject.Inject
import groovy.json.JsonException
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import ratpack.exec.Promise
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse


class DirectoriesService {

    private JsonSlurper jsonSlurper
    private ObjectMapper objectMapper

    @Inject
    DirectoriesService(){
        this.jsonSlurper = new JsonSlurper()
        this.objectMapper = new ObjectMapper()
    }

    Promise<String> listDirectories(HttpClient client,String url, String username, String password, Serializable folderId) {
        try {
            URI uri = getListChildrenUri(url, folderId)
            Promise<ReceivedResponse> responsePromise = sendGetRequest(client, uri, username, password)
            return responsePromise.map({ response ->
                checkResponseStatus(response)
                List<Map<String, Object>> directories = parseJsonResponse(response)
                return JsonOutput.toJson(directories)
            })
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI: ${e.message}")
        }
    }

    Promise<String> listDocuments(HttpClient client,String url, String username, String password, Serializable folderId) {
        try{
            URI uri = "${url}/services/rest/document/listDocuments?folderId=${folderId}".toURI()
            //URI uri = "${url}/services/rest/search/findByFilename?filename=Capture1.pdf".toURI()

            Promise<ReceivedResponse> responsePromise = sendGetRequest(client, uri, username, password)
            return responsePromise.map({ response ->
                checkResponseStatus(response)
                List<Map<String, Object>> directories = parseJsonResponse(response)
                return JsonOutput.toJson(directories)
            })
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI: ${e.message}")
        }
    }

    Promise<ObjectNode> getFolderStructure(HttpClient client,String url, String username, String password, Serializable folderId) {
        URI uri = getListChildrenUri(url, folderId)
        Promise<ReceivedResponse> responsePromise = sendGetRequest(client, uri, username, password)

        return responsePromise.flatMap({ res ->
            checkResponseStatus(res)
            ObjectNode root = JsonNodeFactory.instance.objectNode()
            ArrayNode subFolders = JsonNodeFactory.instance.arrayNode()

            List<Map<String, Object>> directoryList = objectMapper.readValue(res.body.text, List.class)

            for (Map<String, Object> directory : (directoryList as List<Map<String, Object>>)) {
                String id = directory.get("id").toString()
                String name = directory.get("name").toString()

                ObjectNode subFolder = createSubFolderNode(id, name)

                Promise<ObjectNode> subFolderPromise = getFolderStructure(client, url, username, password, id)
                subFolderPromise.then({ subFolderStructure -> subFolder.set("subFolders", subFolderStructure) })
                subFolders.add(subFolder)
            }

            root.put("folderId", folderId.toString())
            root.replace("subFolders", subFolders)

            return Promise.value(root)
        })
    }

    private ObjectNode createSubFolderNode(String id, String name) {
        ObjectNode subFolder = JsonNodeFactory.instance.objectNode()
        subFolder.put("id", id)
        subFolder.put("name", name)
        return subFolder
    }

    URI getListChildrenUri(String baseUrl, Serializable folderId) throws URISyntaxException {
        return buildUri("${baseUrl}", "services", "rest", "folder", "listChildren", "${folderId}")
    }

    private URI buildUri(String baseUrl, String... pathSegments) throws URISyntaxException {
        StringBuilder pathBuilder = new StringBuilder()
        for (String segment : pathSegments) {
            pathBuilder.append("/").append(segment)
            if ("listChildren" == segment) {
                break
            }
        }
        //"${baseUrl}/services/rest/folder/listChildren?folderId=${folderId}"
        return new URI("${baseUrl}${pathBuilder.toString()}?folderId=${pathSegments[pathSegments.length - 1]}")
    }

    private Promise<ReceivedResponse> sendGetRequest(HttpClient client, URI uri, String username, String password) {
        return client.get(uri, { reqSpec ->
            reqSpec.basicAuth(username, password)
            reqSpec.headers.set("Accept", "application/json")
        })
    }

    private void checkResponseStatus(ReceivedResponse response) {
        if (response.statusCode != 200) {
            throw new RuntimeException("Failed to retrieve directory list: ${response.body.text}")
        }
    }

    List<Map<String, Object>> parseJsonResponse(ReceivedResponse response) {
        def parsedJson = parseJson(response.body.text)
        if (parsedJson instanceof List) {
            return parsedJson as List<Map<String, Object>>
        } else {
            throw new RuntimeException("Unexpected JSON structure: ${parsedJson}")
        }
    }

    def parseJson(String jsonString) {
        try {
            return jsonSlurper.parseText(jsonString)
        } catch (JsonException e) {
            throw new RuntimeException("Failed to parse JSON response: ${e.message}")
        }
    }
}
