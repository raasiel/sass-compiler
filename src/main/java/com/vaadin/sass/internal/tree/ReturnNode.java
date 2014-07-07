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
package com.vaadin.sass.internal.tree;

import java.util.Collection;
import java.util.Collections;

import com.vaadin.sass.internal.parser.SassListItem;

public class ReturnNode extends Node implements IVariableNode {

    private SassListItem expr;

    public ReturnNode(SassListItem expression) {
        expr = expression;
    }

    @Override
    public void replaceVariables() {
        expr = expr.replaceVariables();
    }

    @Override
    public Collection<Node> traverse() {
        // need to replace variables here to make sure all vars are in scope
        expr = evaluate();
        return Collections.singleton((Node) this);
    }

    public SassListItem getExpr() {
        return expr;
    }

    /**
     * Evaluate the value of the return node in a context defined by the state
     * of ScssStylesheet (variables currently in scope, defined custom
     * functions).
     * 
     * This method does not modify the ReturnNode itself.
     */
    public SassListItem evaluate() {
        SassListItem expr = getExpr();
        boolean arith = expr.containsArithmeticalOperator();
        expr = expr.replaceVariables();
        expr = expr.evaluateFunctionsAndExpressions(arith);
        return expr;
    }

}
