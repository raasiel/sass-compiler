/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.sass.internal.visitor;

import java.util.Collection;

import com.vaadin.sass.internal.Scope;
import com.vaadin.sass.internal.ScssStylesheet;
import com.vaadin.sass.internal.parser.ParseException;
import com.vaadin.sass.internal.parser.Variable;
import com.vaadin.sass.internal.tree.MixinDefNode;
import com.vaadin.sass.internal.tree.MixinNode;
import com.vaadin.sass.internal.tree.Node;
import com.vaadin.sass.internal.tree.controldirective.TemporaryNode;

public class MixinNodeHandler {

    public static Collection<Node> traverse(MixinNode node)
            throws ParseException {
        return replaceMixins(node);
    }

    private static Collection<Node> replaceMixins(MixinNode node)
            throws ParseException {
        MixinDefNode mixinDef = ScssStylesheet.getMixinDefinition(node
                .getName());
        if (mixinDef == null) {
            throw new ParseException("Mixin Definition: " + node.getName()
                    + " not found");
        }
        return replaceMixinNode(node, mixinDef);
    }

    private static Collection<Node> replaceMixinNode(MixinNode mixinNode,
            MixinDefNode mixinDef) {
        MixinDefNode defClone = mixinDef.copy();

        defClone.replaceContentDirective(mixinNode);

        if (!mixinDef.getArglist().isEmpty()) {
            defClone.replacePossibleArguments(mixinNode.getArglist());
            defClone.replaceVariables();
        }

        // parameters have been evaluated in parent scope, rest should be
        // in the scope where the mixin was defined
        Scope previousScope = ScssStylesheet.openVariableScope(defClone
                .getDefinitionScope());
        try {
            // add variables from argList
            for (Variable var : defClone.getArglist().getArguments()) {
                Variable evaluated = new Variable(var.getName(), var.getExpr()
                        .evaluateFunctionsAndExpressions(true));
                ScssStylesheet.addVariable(evaluated);
            }
            // traverse child nodes in this scope
            // use correct parent with intermediate TemporaryNode
            Node tempParent = new TemporaryNode(mixinNode.getParentNode(),
                    defClone.getChildren());
            return tempParent.traverse();
        } finally {
            ScssStylesheet.closeVariableScope(previousScope);
        }
    }
}
