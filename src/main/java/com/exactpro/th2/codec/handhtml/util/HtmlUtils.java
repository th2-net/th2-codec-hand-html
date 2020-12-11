/*
 Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.exactpro.th2.codec.handhtml.util;


import org.jsoup.nodes.Element;

public class HtmlUtils {

    //TODO: pass function to traversal method

    public static Element findChildTable (Element node) {
        if (node.tagName().equals("table")) {
            return node;
        }

        for (Element child : node.children()) {
            Element subTreeAns = findChildTable(child);

            if (subTreeAns != null) {
                return subTreeAns;
            }
        }

        return null;
    }

    public static Element findChildWithClass (Element node, String className) {
        if (node.className().equals(className)) {
            return node;
        }

        for (Element child : node.children()) {
            Element subTreeAns = findChildWithClass(child, className);

            if (subTreeAns != null) {
                return subTreeAns;
            }
        }

        return null;
    }
}
