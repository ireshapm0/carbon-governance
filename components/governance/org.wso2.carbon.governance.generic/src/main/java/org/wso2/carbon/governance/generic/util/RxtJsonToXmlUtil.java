package org.wso2.carbon.governance.generic.util;

/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

public class RxtJsonToXmlUtil {

    public static JsonObject parse(String jsonLine) {
        JsonParser jsonParser = new JsonParser();
        JsonElement rxtJsonElement = jsonParser.parse(jsonLine);
        JsonObject rxtJsonArtifactTypeObject = rxtJsonElement.getAsJsonObject().getAsJsonObject();
        return rxtJsonArtifactTypeObject;
    }

    /**
     * Returns Artifact Details basic Attributes as a XML
     *
     * @param artifactDetails
     * @return XML String
     */
    public static String artifactDetailsToXML(JsonObject artifactDetails) {

        //Retrieve artifactDetails attributes from JSON model
        JsonElement type = artifactDetails.get("type");
        JsonElement shortName = artifactDetails.get("shortName");
        JsonElement singularLabel = artifactDetails.get("singularLabel");
        JsonElement pluralLabel = artifactDetails.get("pluralLabel");
        JsonElement hasNamespace = artifactDetails.get("hasNamespace");
        JsonElement iconSet = artifactDetails.get("iconSet");
        JsonElement storagePathElem = artifactDetails.get("storagePath");
        String storagePath = storagePathElem.getAsString();
        JsonElement nameAttributeElem = artifactDetails.get("nameAttribute");
        JsonElement namespaceAttributeElem = artifactDetails.get("namespaceAttribute");
        JsonElement lifecycleElem = artifactDetails.get("lifecycle");


        //Mandatory fields
        String artifactXML = "<artifactType type=" + type + " shortName=" + shortName + " singularLabel=" + singularLabel + " pluralLabel=" + pluralLabel +
                             " hasNamespace=" + hasNamespace + " iconSet=" + iconSet + ">\n"
                             + "\t<storagePath>" + storagePath.replaceAll("^\"|\"$", "") + "</storagePath>\n";

        // Non Mandatory fields are added only if they are not empty
        if (!nameAttributeElem.getAsString().replaceAll("^\"|\"$", "").isEmpty()) {
            artifactXML += "\t<nameAttribute>" + nameAttributeElem.getAsString().replaceAll("^\"|\"$", "") + "</nameAttribute>\n";
        }

        if (!namespaceAttributeElem.getAsString().replaceAll("^\"|\"$", "").isEmpty()) {

            artifactXML += "\t<namespaceAttribute>" + namespaceAttributeElem.getAsString().replaceAll("^\"|\"$", "") + "</namespaceAttribute>\n";
        }

        if (!lifecycleElem.getAsString().replaceAll("^\"|\"$", "").isEmpty()) {
            artifactXML += "\t<lifecycle>" + namespaceAttributeElem.getAsString().replaceAll("^\"|\"$", "") + "</lifecycle>\n";
        }

        return (artifactXML);

    }

    /**
     * Returns UI tag in XML
     *
     * @param ui
     * @return <ui> XML
     */
    public static String UIToXML(JsonArray ui) {

        String uiXML = "";
        for (int n = 0; n < ui.size(); n++) {

            JsonObject columnObject = ui.get(n).getAsJsonObject();

            JsonElement type = columnObject.get("type");
            JsonElement value = columnObject.get("value");
            JsonElement href = columnObject.get("href");
            String[] columnName = value.getAsString().replaceAll("^\"|\"$", "").split("_");
            uiXML += "\t\t<column name=\"" + columnName[1].toUpperCase() + "\">\n" + "\t\t\t<data type=" + type + " value=" + value + " href=" + href + "/>\n" + "\t\t</column>\n";
        }
        if (ui.size() != 0) {
            String UIXML = "<ui>\n\t<list>\n" + uiXML + "\t</list>\n</ui>";
            return UIXML;
        } else {
            return (uiXML);
        }
    }

    /**
     * Returns relationships in xml
     *
     * @param relationship
     * @return <relationships> in xml
     */
    public static String relationshipToXML(JsonArray relationship) {

        //relationship content to XML
        String relationshipXML = "";
        if (relationship.size() > 0) {
            relationshipXML += "<relationships>\n";
            for (int n = 0; n < relationship.size(); n++) {

                JsonObject columnObject = relationship.get(n).getAsJsonObject();
                if ((columnObject.get("relationship")).getAsString().replaceAll("^\"|\"$", "").equals("Association")) {


                    JsonElement type = columnObject.get("type");
                    JsonElement source = columnObject.get("source/target");
                    relationshipXML = relationshipXML + "\t<association " + "type=" + type + " source=" + source + "/>\n";
                } else {

                    JsonElement dtype = columnObject.get("type");
                    JsonElement dsource = columnObject.get("source/target");
                    relationshipXML = relationshipXML + "\t<dependency " + "type=" + dtype + " target=" + dsource + "/>\n";
                }

            }
            relationshipXML += "</relationships>";
        }
        return (relationshipXML);
    }

    /**
     * Returns Content tag in xml
     *
     * @param content
     * @return <content> in xml
     */
    public static String contentToXML(JsonObject content) {

        String contentXML = "<content>\n";
        for (Map.Entry<String, JsonElement> entry : content.entrySet()) {
            JsonElement value = entry.getValue();
            JsonArray tableElements = value.getAsJsonArray();
            if (tableElements.size() != 0) {
                String key = entry.getKey();
                contentXML += "\t<table name=\"" + key + "\">\n";
                for (int i = 0; i < tableElements.size(); i++) {
                    JsonObject contentElement = tableElements.get(i).getAsJsonObject();
                    if (contentElement.get("type").getAsString().replaceAll("^\"|\"$", "").equals("text")) {
                        String name = contentElement.get("label").getAsString().replaceAll("^\"|\"$", "");
                        if (contentElement.get("required").isJsonNull()) {
                            contentXML += "\t\t<field type =\"text\">\n";
                            contentXML += "\t\t\t<name>" + name + "</name>\n";
                            contentXML += "\t\t</field>\n";

                        } else {
                            contentXML += "\t\t<field type =\"text\" required=\"true\">\n";
                            contentXML += "\t\t\t<name>" + name + "</name>\n";
                            contentXML += "\t\t</field>\n";
                        }

                    } else if (contentElement.get("type").getAsString().replaceAll("^\"|\"$", "").equals("textArea")) {

                        String name = contentElement.get("label").getAsString().replaceAll("^\"|\"$", "");
                        contentXML += "\t\t<field type =\"text-area\">\n";
                        contentXML += "\t\t\t<name>" + name + "</name>\n";
                        contentXML += "\t\t</field>\n";

                    } else if (contentElement.get("type").getAsString().replaceAll("^\"|\"$", "").equals("option")) {
                        String name = contentElement.get("label").getAsString().replaceAll("^\"|\"$", "");
                        String options = contentElement.get("options").getAsString().replaceAll("^\"|\"$", "");
                        String[] options_split = options.split("\n");
                        System.out.println(options_split);

                        contentXML += "\t\t<field type=\"options\">\n";
                        contentXML += "\t\t\t<name label=\"type\">" + name + "</name>\n";
                        if (options_split.length > 0) {
                            contentXML += "\t\t\t<values>\n";
                            for (int j = 0; j < options_split.length; j++) {
                                contentXML += "\t\t\t\t<value>" + options_split[j] + "</value>\n";
                            }
                            contentXML += "\t\t\t</values>\n";
                        }
                        contentXML += "\t\t</field>\n";
                    }
                }
                contentXML += "\t</table>\n";
            }
        }
        contentXML += "</content>";
        return contentXML;
    }


    public static void writeToFile(String name, String XML) {
        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(name.replace("\"", "") + ".rxt");
            printWriter.println(XML);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }

    }
}
