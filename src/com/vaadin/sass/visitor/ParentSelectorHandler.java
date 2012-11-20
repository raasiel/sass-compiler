/*
 * Copyright 2011 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.sass.visitor;

import java.util.ArrayList;
import java.util.HashMap;

import com.vaadin.sass.ScssStylesheet;
import com.vaadin.sass.tree.BlockNode;
import com.vaadin.sass.tree.Node;

public class ParentSelectorHandler {

    public static void traverse(BlockNode block) throws Exception {
        Node parentNode = block.getParentNode();
        if (parentNode instanceof BlockNode) {
            boolean isParentSelector = false;
            ArrayList<String> newList = new ArrayList<String>(block
                    .getSelectorList().size());
            BlockNode parentBlock = (BlockNode) parentNode;
            for (final String s : block.getSelectorList()) {

                if (s.contains("&")) {
                    for (final String parentSelector : parentBlock
                            .getSelectorList()) {
                        newList.add(s.replace("&", parentSelector));
                        isParentSelector = true;
                    }

                }
            }

            if (isParentSelector) {
                block.setSelectorList(newList);
                Node oldparent = block.getParentNode();
                HashMap<Node, Node> lastNodeAdded = ScssStylesheet
                        .getLastNodeAdded();
                if (lastNodeAdded.containsKey(block.getParentNode())) {
                    block.getParentNode()
                            .getParentNode()
                            .appendChild(block,
                                    lastNodeAdded.get(block.getParentNode()));
                } else {
                    block.getParentNode().getParentNode()
                            .appendChild(block, block.getParentNode());
                }

                lastNodeAdded.put(oldparent, block);
            }
        }
    }
}