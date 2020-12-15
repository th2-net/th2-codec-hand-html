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


import com.exactpro.th2.codec.handhtml.processor.FixHtmlProcessorConfiguration;
import org.jsoup.nodes.Element;

import java.util.*;

public class HtmlUtils {

    /*
        Parses html table and constructs
        multi-layer map hierarchy from it
    */

    public static Map<String, Object> parse(Element table, FixHtmlProcessorConfiguration configuration) {
        Element tableBody = table.child(1);

        Stack<Map<String, Object>> stack = new Stack<>();
        stack.add(new HashMap<>());

        int prevDepth = configuration.getHierarchyStart(), curDepth;
        for (Element row : tableBody.children()) {

            Element fieldName = row.child(0);

            // Indicator that internal fields will be coming
            if (row.childrenSize() == 1) {
                curDepth = Integer.parseInt(fieldName.child(0).attr(configuration.getHierarchyAttribute())
                        .split(configuration.getHierarchyIndicatorPrefix())[1]
                        .split(configuration.getHierarchyIndicatorSuffix())[0]);

                /*
                    Pop maps from stack, because their fields are over
                 */
                while (curDepth < prevDepth) {
                    stack.pop();
                    prevDepth -= configuration.getHierarchyStep();
                }
                prevDepth = curDepth;


                /*
                    Creation of hierarchy's new layer
                    and adding it as one of the fields of last Map
                 */
                Map<String, Object> childHMap = new HashMap<>();
                stack.peek().put(fieldName.text(), childHMap);

                stack.add(childHMap);
                continue;
            }

            curDepth = Integer.parseInt(fieldName.attr(configuration.getHierarchyAttribute())
                    .split(configuration.getHierarchyIndicatorPrefix())[1]
                    .split(configuration.getHierarchyIndicatorSuffix())[0]);

            /*
                Same logic as above
             */
            while (curDepth < prevDepth) {
                stack.pop();
                prevDepth -= configuration.getHierarchyStep();
            }
            prevDepth = curDepth;

            /*
                At this point it's guaranteed that
                we are putting non-complex value
             */
            Element fieldValue = row.child(1);
            stack.peek().put(fieldName.text(), fieldValue.text());
        }


        /*
            The first map will be the root Map
         */
        while (stack.size() > 1) {
            stack.pop();
        }

        return stack.peek();
    }

    /*
        Function which traverses and finds
        node with given criteria
     */

    public static Element traverseSubtree (Element node, CriteriaChecker criteriaChecker) {
        if (criteriaChecker.checkCriteria(node)) {
            return node;
        }

        for (Element child : node.children()) {
            Element subTreeAns = traverseSubtree(child, criteriaChecker);

            if (subTreeAns != null) {
                return subTreeAns;
            }
        }

        return null;
    }

    /*
        Classes which implement this interface
        will be passed to node traversal function
        to return node with desired specifications
     */

    public interface CriteriaChecker {
        boolean checkCriteria (Element node);
    }

    public static class TagNameChecker implements CriteriaChecker {
        private final String tagName;

        public TagNameChecker (String... criteria) {
            this.tagName = Arrays.stream(criteria).findFirst().get();
        }

        @Override
        public boolean checkCriteria(Element node) {
            return node.tagName().equals(tagName);
        }
    }

    public static class ClassNameChecker implements CriteriaChecker {
        private final String className;

        public ClassNameChecker (String... criteria) {
            this.className = Arrays.stream(criteria).findFirst().get();
        }

        @Override
        public boolean checkCriteria(Element node) {
            return node.className().equals(className);
        }
    }
}
