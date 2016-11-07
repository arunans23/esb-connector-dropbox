/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.connector.integration.test.dropbox;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.connector.integration.test.base.ConnectorIntegrationTestBase;
import org.wso2.connector.integration.test.base.RestResponse;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class DropboxConnectorIntegrationTest extends ConnectorIntegrationTestBase {

    private Map<String, String> esbRequestHeadersMap = new HashMap<String, String>();
    private Map<String, String> apiRequestHeadersMap = new HashMap<String, String>();
    private Map<String, String> apiRequestHeadersMapDownload = new HashMap<String, String>();
    private Map<String, String> headersMap = new HashMap<String, String>();

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        init("dropbox-connector-2.0.0");
        esbRequestHeadersMap.put("Accept-Charset", "UTF-8");
        esbRequestHeadersMap.put("Content-Type", "application/json");

        apiRequestHeadersMap.put("Accept-Charset", "UTF-8");
        apiRequestHeadersMap.put("Content-Type", "application/json");
        apiRequestHeadersMap.put("Authorization", "Bearer " + connectorProperties.getProperty("accessToken"));
        apiRequestHeadersMapDownload.put("Accept-Charset", "UTF-8");
        apiRequestHeadersMapDownload.put("Authorization", "Bearer " + connectorProperties.getProperty("accessToken"));
    }

    /**
     * Positive test case for createFolder method with mandatory parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 2, description = "dropbox {createFolder} integration test with mandatory parameters.")
    public void testCreateFolderWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:createFolder");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_createFolder_mandatory.json");
        // Direct API Call - Retrieves meta data of the created folder to compare with
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_createFolder_mandatory.json");
        Assert.assertEquals(esbRestResponse.getBody().get("id").toString(), apiRestResponse.getBody()
                .get("id").toString());
        Assert.assertEquals(apiRestResponse.getBody().get(".tag").toString(), "folder");
    }

    /**
     * Positive test case for createFolder method with optional parameters.
     */
    @Test(priority = 2, dependsOnMethods = {"testCreateFolderWithMandatoryParameters"},
            description = "dropbox {createFolder} integration test with optional parameters.")
    public void testCreateFolderWithOptionalParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:createFolder");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_createFolder_optional.json");
        connectorProperties.setProperty("optionalPath", esbRestResponse.getBody().get("path_display").toString());
        // Direct API Call - Retrieves meta data of the created folder to compare with
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_createFolder_optional.json");
        Assert.assertEquals(esbRestResponse.getBody().get("id").toString(), apiRestResponse.getBody()
                .get("id").toString());
        Assert.assertEquals(apiRestResponse.getBody().get(".tag").toString(), "folder");
    }


    /**
     * Negative test case for createFolder method. Negative scenario is checked by trying to create already
     * existing folder created by testCreateFolderWithMandatoryParameters method
     */
    @Test(priority = 2, dependsOnMethods = {"testCreateFolderWithOptionalParameters"},
            description = "dropbox {createFolder} integration test with negative case.")
    public void testCreateFolderWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:createFolder");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_createFolder_negative.json");
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 409);
        Assert.assertEquals(esbRestResponse.getBody().getJSONObject("error").get(".tag").toString(), "path");
    }

    /**
     * Positive test case for uploadFile
     *
     * @throws NoSuchAlgorithmException
     */
    @Test(priority = 1, dependsOnMethods = {"testCreateFolderWithNegativeCase"},
            description = "dropbox {uploadFile} integration test positive case.")
    public void testUploadFile() throws IOException, JSONException, NoSuchAlgorithmException {
        headersMap.put("Action", "urn:uploadFile");
        headersMap.put("Content-Type", "application/octet-stream");
        String requestString = proxyUrl + "?apiUrl=" + connectorProperties.getProperty("contentApiUrl")
                + "&accessToken=" + connectorProperties.getProperty("accessToken")
                + "&apiVersion=" + connectorProperties.getProperty("apiVersion")
                + "&path=" + connectorProperties.getProperty("folderName1")
                + "/" + connectorProperties.getProperty("fileName")
                + "&mode=" + connectorProperties.getProperty("mode")
                + "&mute=" + connectorProperties.getProperty("mute");

        MultipartFormdataProcessor multipartProcessor = new MultipartFormdataProcessor(requestString, headersMap);
        File file = new File(pathToResourcesDirectory + connectorProperties.getProperty("uploadSourcePath"));
        multipartProcessor.addFiletoRequestBody(file);
        RestResponse<JSONObject> esbRestResponse = multipartProcessor.processAttachmentForJsonResponse();
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 200);
        Assert.assertTrue(esbRestResponse.getBody().has("id"));
    }

    /**
     * Positive test case for getTemporaryLink method with mandatory parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 2, dependsOnMethods = {"testUploadFile"},
            description = "dropbox {getTemporaryLink} integration test with mandatory parameters.")
    public void testGetTemporaryLinkWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:getTemporaryLink");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_getTemporaryLink_mandatory.json");
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 200);
        Assert.assertTrue(esbRestResponse.getBody().has("link"));
    }

    /**
     * Negative test case for getTemporaryLink method.
     */
    @Test(priority = 2, dependsOnMethods = {"testGetTemporaryLinkWithMandatoryParameters"},
            description = "dropbox {getTemporaryLink} integration test with negative case.")
    public void testGetTemporaryLinkWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:getTemporaryLink");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_getTemporaryLink_negative.json");
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 409);
        Assert.assertEquals(esbRestResponse.getBody().getJSONObject("error").get(".tag").toString(), "path");
    }

    /**
     * Positive test case for copy method with mandatory parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 2, dependsOnMethods = {"testUploadFile"},
            description = "dropbox {copy} integration test with mandatory parameters.")
    public void testCopyWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:copy");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_copy_mandatory.json");
        // Direct API Call - Retrieves meta data of the copied file or folder to compare with
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_copy_mandatory.json");
        Assert.assertEquals(esbRestResponse.getBody().get("id").toString(), apiRestResponse.getBody()
                .get("id").toString());
        Assert.assertEquals(apiRestResponse.getBody().get(".tag").toString(), "file");
    }

    /**
     * Positive test case for copy method with optional parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 2, dependsOnMethods = {"testCopyWithMandatoryParameters"},
            description = "dropbox {copy} integration test with optional parameters.")
    public void testCopyWithOptionalParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:copy");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_copy_optional.json");
        connectorProperties.setProperty("optionalFilePath", esbRestResponse.getBody().get("path_display").toString());
        // Direct API Call - Retrieves meta data of the created folder to compare with
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_copy_optional.json");
        Assert.assertEquals(esbRestResponse.getBody().get("id").toString(), apiRestResponse.getBody()
                .get("id").toString());
        Assert.assertEquals(apiRestResponse.getBody().get(".tag").toString(), "file");
    }

    /**
     * Negative test case for copy method. Negative scenario is checked by trying to copy a file or folder to
     * a place which tries to override an existing file or folder
     */
    @Test(priority = 2, dependsOnMethods = {"testCopyWithOptionalParameters"},
            description = "dropbox {copy} integration test with negative case.")
    public void testCopyWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:copy");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_copy_negative.json");
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 409);
        Assert.assertEquals(esbRestResponse.getBody().getJSONObject("error").get(".tag").toString(), "to");
    }

    /**
     * Positive test case for getMetadata method with mandatory parameters.
     */
    @Test(priority = 1, dependsOnMethods = {"testUploadFile"},
            description = "dropbox {getMetadata} integration test with mandatory parameters.")
    public void testGetMetadataWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:getMetadata");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "esb_getMetadata_mandatory.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_getMetadata_mandatory.json");
        Assert.assertEquals(esbRestResponse.getBody().get(".tag"), apiRestResponse.getBody().get(".tag"));
        Assert.assertEquals(esbRestResponse.getBody().get("name"), apiRestResponse.getBody().get("name"));
        Assert.assertEquals(esbRestResponse.getBody().get("id"), apiRestResponse.getBody().get("id"));
    }

    /**
     * Positive test case for getMetadata method with optional parameters.
     */
    @Test(priority = 1, dependsOnMethods = {"testGetMetadataWithMandatoryParameters"},
            description = "dropbox {getMetadata} integration test with optional parameters.")
    public void testGetMetadataWithOptionalParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:getMetadata");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "esb_getMetadata_optional.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_getMetadata_optional.json");
        Assert.assertEquals(esbRestResponse.getBody().get(".tag"), apiRestResponse.getBody().get(".tag"));
        Assert.assertEquals(esbRestResponse.getBody().get("name"), apiRestResponse.getBody().get("name"));
        Assert.assertEquals(esbRestResponse.getBody().get("id"), apiRestResponse.getBody().get("id"));
        Assert.assertEquals(esbRestResponse.getBody().toString(), apiRestResponse.getBody().toString());
    }

    /**
     * Negative test case for getMetadata method.
     */
    @Test(priority = 1, dependsOnMethods = {"testGetMetadataWithOptionalParameters"},
            description = "dropbox {getMetadata} integration test negative case.")
    public void testGetMetadataWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:getMetadata");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_getMetadata_negative.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_getMetadata_negative.json");
        Assert.assertEquals(esbRestResponse.getBody().get("error").toString(), apiRestResponse.getBody().get("error").toString());
    }

    /**
     * Positive test case for createSharedLinkWithSettings method with mandatory parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 1, dependsOnMethods = {"testGetMetadataWithNegativeCase"},
            description = "dropbox {createSharedLinkWithSettings} integration test with mandatory parameters.")
    public void testCreateDirectLinkWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:createSharedLinkWithSettings");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_createSharedLinkWithSettings_mandatory.json");
        String sharedURL = esbRestResponse.getBody().get("url").toString();
        connectorProperties.setProperty("sharedURL", sharedURL);
        // Direct API Call - Retrieves directLink data
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/sharing/get_shared_link_metadata";
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_createSharedLinkWithSettings_mandatory.json");
        String rev = apiRestResponse.getBody().get("rev").toString();
        connectorProperties.setProperty("rev", rev);
        Assert.assertEquals(esbRestResponse.getBody().get("url").toString(), apiRestResponse.getBody().get("url")
                .toString());
    }

    /**
     * Positive test case for createSharedLinkWithSettings method with optional parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 1, dependsOnMethods = {"testCreateDirectLinkWithMandatoryParameters"},
            description = "dropbox {createSharedLinkWithSettings} integration test with optional parameters.")
    public void testCreateDirectLinkWithOptionalParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:createSharedLinkWithSettings");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_createSharedLinkWithSettings_optional.json");
        connectorProperties.setProperty("sharedURL", esbRestResponse.getBody().get("url").toString());
        // Direct API Call - Retrieves directLink data
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/sharing/get_shared_link_metadata";
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_createSharedLinkWithSettings_mandatory.json");
        Assert.assertEquals(esbRestResponse.getBody().get("url").toString(), apiRestResponse.getBody().get("url")
                .toString());
    }

    /**
     * Negative test case for createSharedLinkWithSettings method.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 1, dependsOnMethods = {"testCreateDirectLinkWithOptionalParameters"},
            description = "dropbox {createSharedLinkWithSettings} integration test with negative parameters.")
    public void testCreateDirectLinkWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:createSharedLinkWithSettings");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_createSharedLinkWithSettings_negative.json");
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 409);
        Assert.assertTrue(esbRestResponse.getBody().has("error_summary"));
    }

    /**
     * Positive test case for listRevisions method with mandatory parameters.
     */
    @Test(priority = 1, dependsOnMethods = {"testCreateDirectLinkWithMandatoryParameters"},
            description = "dropbox {listRevisions} integration test with mandatory parameters.")
    public void testListRevisionsWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:listRevisions");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/list_revisions";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_listRevisions_mandatory.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_listRevisions_mandatory.json");
        Assert.assertEquals(esbRestResponse.getBody().toString(), apiRestResponse.getBody().toString());
    }

    /**
     * Positive test case for listRevisions method with optional parameters.
     */
    @Test(priority = 1, dependsOnMethods = {"testListRevisionsWithMandatoryParameters"},
            description = "dropbox {listRevisions} integration test with optional parameters.")
    public void testListRevisionsWithOptionalParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:listRevisions");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/list_revisions";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_listRevisions_optional.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_listRevisions_optional.json");
        Assert.assertEquals(esbRestResponse.getBody().toString(), apiRestResponse.getBody().toString());
    }

    /**
     * Negative test case for listRevisions method.
     */
    @Test(priority = 1, dependsOnMethods = {"testListRevisionsWithOptionalParameters"},
            description = "dropbox {listRevisions} integration test with negative case.")
    public void testListRevisionsWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:listRevisions");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/list_revisions";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_listRevisions_negative.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_listRevisions_negative.json");
        Assert.assertEquals(esbRestResponse.getBody().getJSONObject("error").toString(),
                apiRestResponse.getBody().getJSONObject("error").toString());
    }

    /**
     * Positive test case for search method with mandatory parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 1, dependsOnMethods = {"testListRevisionsWithOptionalParameters"},
            description = "dropbox {search} integration test with mandatory parameters.")
    public void testSearchWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:search");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/search";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_search_mandatory.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_search_mandatory.json");
        Assert.assertEquals(esbRestResponse.getBody().toString(), apiRestResponse.getBody().toString());
    }

    /**
     * Positive test case for search method with optional parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 1, dependsOnMethods = {"testSearchWithMandatoryParameters"},
            description = "dropbox {search} integration test with optional parameters.")
    public void testSearchWithOptionalParameters() throws IOException, JSONException {

        esbRequestHeadersMap.put("Action", "urn:search");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/search";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_search_optional.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_search_optional.json");
        Assert.assertEquals(esbRestResponse.getBody().toString(), apiRestResponse
                .getBody().toString());
    }

    /**
     * search method negative case.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 1, groups = {"wso2.esb"}, dependsOnMethods = {"testSearchWithOptionalParameters"},
            description = "dropbox {search} integration test negative case.")
    public void testSearchWithNegativeCase() throws IOException, JSONException {

        esbRequestHeadersMap.put("Action", "urn:search");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/search";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_search_negative.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_search_negative.json");
        Assert.assertEquals(esbRestResponse.getBody().getJSONObject("error").toString(),
                apiRestResponse.getBody().getJSONObject("error").toString());
    }

    /**
     * Positive test case for move method with mandatory parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 2, dependsOnMethods = {"testSearchWithOptionalParameters"},
            description = "dropbox {move} integration test with mandatory parameters.")
    public void testMoveWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:move");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> firstApiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_move_mandatory.json");
        sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "esb_move_mandatory.json");
        RestResponse<JSONObject> secondApiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_move_mandatory.json");
        Assert.assertTrue(!firstApiRestResponse.getBody().has("error") && secondApiRestResponse.getBody().has("error"));
    }

    /**
     * Positive test case for move method with optional parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 2, dependsOnMethods = {"testMoveWithMandatoryParameters"},
            description = "dropbox {move} integration test with optional parameters.")
    public void testMoveWithOptionalParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:move");
        String apiEndPoint =
                connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> firstApiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_move_optional.json");
        sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "esb_move_optional.json");
        RestResponse<JSONObject> secondApiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_move_optional.json");
        Assert.assertTrue(!firstApiRestResponse.getBody().has("error") && secondApiRestResponse.getBody().has("error"));
    }

    /**
     * Negative test case for move method.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(priority = 2, dependsOnMethods = {"testMoveWithOptionalParameters"},
            description = "dropbox {move} integration test negative case.")
    public void testMoveNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:move");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/move";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_move_optional.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_move_negative.json");
        Assert.assertEquals(esbRestResponse.getBody().getJSONObject("error").toString(),
                apiRestResponse.getBody().getJSONObject("error").toString());
    }

    /**
     * Positive test case for delete method with mandatory parameters.
     */
    @Test(priority = 2, dependsOnMethods = {"testMoveWithOptionalParameters"},
            description = "dropbox {delete} integration test with mandatory parameters.")
    public void testDeleteWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:delete");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> firstApiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_delete_mandatory.json");
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_delete_mandatory.json");
        String rev = esbRestResponse.getBody().get("rev").toString();
        connectorProperties.setProperty("rev", rev);
        RestResponse<JSONObject> secondApiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_delete_mandatory.json");
        Assert.assertTrue(!firstApiRestResponse.getBody().has("error") && secondApiRestResponse.getBody().has("error"));
    }

    /**
     * Negative test case for delete method.
     */
    @Test(priority = 2, dependsOnMethods = {"testDeleteWithMandatoryParameters"},
            description = "dropbox {delete} integration test with negative case.")
    public void testDeleteWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:delete");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/delete";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_delete_negative.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_delete_negative.json");
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), apiRestResponse.getHttpStatusCode());
        Assert.assertEquals(esbRestResponse.getBody().getJSONObject("error").toString(), apiRestResponse.getBody()
                .getJSONObject("error").toString());
    }

    /**
     * Positive test case for restoreFile method with mandatory parameters.
     */
    @Test(priority = 1, dependsOnMethods = {"testDeleteWithMandatoryParameters"},
            description = "dropbox {restoreFile} integration test with mandatory parameters.")
    public void testRestoreFileWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:restoreFile");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/get_metadata";
        RestResponse<JSONObject> firstApiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_delete_mandatory.json");
        sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "esb_restoreFile_mandatory.json");
        RestResponse<JSONObject> secondApiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_delete_mandatory.json");
        Assert.assertTrue(firstApiRestResponse.getBody().has("error") && !secondApiRestResponse.getBody().has("error"));
    }

    /**
     * Negative test case for restoreFile method.
     */
    @Test(priority = 1, dependsOnMethods = {"testRestoreFileWithMandatoryParameters"},
            description = "dropbox {restoreFile} integration test negative case.")
    public void testRestoreFileWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:restoreFile");
        String apiEndPoint = connectorProperties.getProperty("dropboxApiUrl") + "/2/files/restore";
        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
                "esb_restoreFile_negative.json");
        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "POST", apiRequestHeadersMap,
                "api_restoreFile_negative.json");
        Assert.assertEquals(esbRestResponse.getBody().toString(), apiRestResponse.getBody().toString());
    }
//
//    /**
//     * Positive test case for download method with mandatory parameters.
//     *
//     * @throws JSONException
//     * @throws IOException
//     */
//    @Test(priority = 1, description = "dropbox {download} integration test with mandatory parameters.")
//    public void testDownloadWithMandatoryParameters() throws IOException, JSONException {
//        esbRequestHeadersMap.put("Action", "urn:download");
//        String apiEndPoint = connectorProperties.getProperty("contentApiUrl") + "/2/files/download";
//        RestResponse<JSONObject> esbRestResponse = sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap,
//                "esb_download_mandatory.json");
//        String apiArg = "{\"path\": \"" + connectorProperties.getProperty("folderName1") + "/"
//                + connectorProperties.getProperty("fileName") + "\"}";
//        apiRequestHeadersMapDownload.put("Dropbox-API-Arg", apiArg);
//        RestResponse<JSONObject> apiRestResponse = sendJsonRestRequest(apiEndPoint, "GET", apiRequestHeadersMapDownload);
//        Assert.assertEquals(esbRestResponse.getHeadersMap().get("x-dropbox-metadata"), apiRestResponse.getHeadersMap()
//                .get("x-dropbox-metadata"));
//         Assert.assertEquals(esbRestResponse.getBody().get("output"), apiRestResponse.getBody().get("output"));
//
//    }
}
