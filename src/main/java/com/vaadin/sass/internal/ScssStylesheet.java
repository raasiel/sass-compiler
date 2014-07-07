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

package com.vaadin.sass.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.InputSource;

import com.vaadin.sass.internal.handler.SCSSDocumentHandler;
import com.vaadin.sass.internal.handler.SCSSDocumentHandlerImpl;
import com.vaadin.sass.internal.handler.SCSSErrorHandler;
import com.vaadin.sass.internal.parser.ParseException;
import com.vaadin.sass.internal.parser.Parser;
import com.vaadin.sass.internal.parser.SCSSParseException;
import com.vaadin.sass.internal.parser.Variable;
import com.vaadin.sass.internal.resolver.ClassloaderResolver;
import com.vaadin.sass.internal.resolver.FilesystemResolver;
import com.vaadin.sass.internal.resolver.ScssStylesheetResolver;
import com.vaadin.sass.internal.tree.FunctionDefNode;
import com.vaadin.sass.internal.tree.MixinDefNode;
import com.vaadin.sass.internal.tree.Node;
import com.vaadin.sass.internal.visitor.ExtendNodeHandler;

public class ScssStylesheet extends Node {

    private static final long serialVersionUID = 3849790204404961608L;

    @Deprecated
    private static ScssStylesheet mainStyleSheet = null;

    private static Scope scope = new Scope();

    private File file;

    private String charset;

    private List<ScssStylesheetResolver> resolvers = new ArrayList<ScssStylesheetResolver>();

    // relative path to use when importing files etc.
    private String prefix = "";

    private List<String> sourceUris = new ArrayList<String>();

    /**
     * Read in a file SCSS and parse it into a ScssStylesheet
     * 
     * @param file
     * @throws IOException
     */
    public ScssStylesheet() {
        super();
    }

    /**
     * Main entry point for the SASS compiler. Takes in a file and builds up a
     * ScssStylesheet tree out of it. Calling compile() on it will transform
     * SASS into CSS. Calling printState() will print out the SCSS/CSS.
     * 
     * @param identifier
     *            The file path. If null then null is returned.
     * @return
     * @throws CSSException
     * @throws IOException
     */
    public static ScssStylesheet get(String identifier) throws CSSException,
            IOException {
        return get(identifier, null);
    }

    /**
     * Main entry point for the SASS compiler. Takes in a file and an optional
     * parent style sheet, then builds up a ScssStylesheet tree out of it.
     * Calling compile() on it will transform SASS into CSS. Calling
     * printState() will print out the SCSS/CSS.
     * 
     * @param identifier
     *            The file path. If null then null is returned.
     * @param parentStylesheet
     *            Style sheet from which to inherit resolvers and encoding. May
     *            be null.
     * @return
     * @throws CSSException
     * @throws IOException
     */
    public static ScssStylesheet get(String identifier,
            ScssStylesheet parentStylesheet) throws CSSException, IOException {
        return get(identifier, parentStylesheet, new SCSSDocumentHandlerImpl(),
                new SCSSErrorHandler());
    }

    /**
     * Main entry point for the SASS compiler. Takes in a file, an optional
     * parent stylesheet, and document and error handlers. Then builds up a
     * ScssStylesheet tree out of it. Calling compile() on it will transform
     * SASS into CSS. Calling printState() will print out the SCSS/CSS.
     * 
     * @param identifier
     *            The file path. If null then null is returned.
     * @param parentStylesheet
     *            Style sheet from which to inherit resolvers and encoding. May
     *            be null.
     * @param documentHandler
     *            Instance of document handler. May not be null.
     * @param errorHandler
     *            Instance of error handler. May not be null.
     * @return
     * @throws CSSException
     * @throws IOException
     */
    public static ScssStylesheet get(String identifier,
            ScssStylesheet parentStylesheet,
            SCSSDocumentHandler documentHandler, SCSSErrorHandler errorHandler)
            throws CSSException, IOException {
        /*
         * The encoding to be used is passed through "encoding" parameter. the
         * imported children scss node will have the same encoding as their
         * parent, ultimately the root scss file. The root scss node has this
         * "encoding" parameter to be null. Its encoding is determined by the
         * 
         * @charset declaration, the default one is ASCII.
         */

        if (identifier == null) {
            return null;
        }

        // FIXME Is this actually intended? /John 1.3.2013
        File file = new File(identifier);
        file = file.getCanonicalFile();

        ScssStylesheet stylesheet = documentHandler.getStyleSheet();
        if (parentStylesheet == null) {
            // Use default resolvers
            stylesheet.addResolver(new FilesystemResolver());
            stylesheet.addResolver(new ClassloaderResolver());
        } else {
            // Use parent resolvers
            stylesheet.setResolvers(parentStylesheet.getResolvers());
        }
        InputSource source = stylesheet.resolveStylesheet(identifier,
                parentStylesheet);
        if (source == null) {
            return null;
        }
        if (parentStylesheet != null) {
            source.setEncoding(parentStylesheet.getCharset());
        }

        Parser parser = new Parser();
        parser.setErrorHandler(errorHandler);
        parser.setDocumentHandler(documentHandler);

        try {
            parser.parseStyleSheet(source);
        } catch (ParseException e) {
            // catch ParseException, re-throw a SCSSParseException which has
            // file name info.
            throw new SCSSParseException(e, identifier);
        }

        stylesheet.setCharset(parser.getInputSource().getEncoding());
        stylesheet.sourceUris.add(source.getURI());

        return stylesheet;
    }

    public InputSource resolveStylesheet(String identifier,
            ScssStylesheet parentStylesheet) {
        for (ScssStylesheetResolver resolver : getResolvers()) {
            InputSource source = resolver.resolve(parentStylesheet, identifier);
            if (source != null) {
                File f = new File(source.getURI());
                setFile(f);
                return source;
            }
        }

        return null;
    }

    /**
     * Retrieves a list of resolvers to use when resolving imports
     * 
     * @return the resolvers used to resolving imports
     */
    public List<ScssStylesheetResolver> getResolvers() {
        return Collections.unmodifiableList(resolvers);
    }

    /**
     * Sets the list of resolvers to use when resolving imports
     * 
     * @param resolvers
     *            the resolvers to set
     */
    public void setResolvers(List<ScssStylesheetResolver> resolvers) {
        this.resolvers = new ArrayList<ScssStylesheetResolver>(resolvers);
    }

    /**
     * Adds the given resolver to the resolver list
     * 
     * @param resolver
     *            The resolver to add
     */
    public void addResolver(ScssStylesheetResolver resolver) {
        resolvers.add(resolver);
    }

    public List<String> getSourceUris() {
        return Collections.unmodifiableList(sourceUris);
    }

    public void addSourceUris(Collection<String> uris) {
        sourceUris.addAll(uris);
    }

    /**
     * Applies all the visitors and compiles SCSS into Css.
     * 
     * @throws Exception
     */
    public void compile() throws Exception {
        // reset compilation state
        mainStyleSheet = this;
        // top-level scope has definitions by parser
        scope = new Scope();
        ExtendNodeHandler.clear();

        traverse();
        ExtendNodeHandler.modifyTree(this);
    }

    public static void defineFunction(FunctionDefNode function) {
        scope.defineFunction(function);
    }

    public static void defineMixin(MixinDefNode mixin) {
        scope.defineMixin(mixin);
    }

    /**
     * Prints out the current state of the node tree. Will return SCSS before
     * compile and CSS after.
     * 
     * For now this is an own method with it's own implementation that most node
     * types will implement themselves.
     */
    @Override
    public String printState() {
        return buildString(PRINT_STRATEGY);
    }

    @Override
    public String toString() {
        return "Stylesheet node [" + buildString(TO_STRING_STRATEGY) + "]";
    }

    public static ScssStylesheet get() {
        return mainStyleSheet;
    }

    /**
     * Traverses a node and its children recursively, calling all the
     * appropriate handlers via {@link Node#traverse()}.
     * 
     * The node itself may be removed during the traversal and replaced with
     * other nodes at the same position or later on the child list of its
     * parent.
     */
    @Override
    public Collection<Node> traverse() {
        traverseChildren();
        return Collections.singleton((Node) this);
    }

    /**
     * Switch to a new sub-scope of a specific scope. Any variables created
     * after opening a new scope are only valid until the scope is closed,
     * whereas variables from the parent scope(s) that are modified will keep
     * their new values even after closing the inner scope.
     * 
     * When using this method, the scope must be closed with
     * {@link #closeVariableScope(Scope)} with the return value of this method
     * as its parameter.
     * 
     * @return previous scope
     */
    public static Scope openVariableScope(Scope parent) {
        Scope previousScope = scope;
        scope = new Scope(parent);
        return previousScope;
    }

    /**
     * End a scope for variables, removing all active variables that only
     * existed in the new scope.
     */
    public static void closeVariableScope(Scope newScope) {
        scope = newScope;
    }

    /**
     * Returns the current scope. The returned value should be treated as opaque
     * and only used as a parameter to {@link #openVariableScope(Scope)}.
     * 
     * @return current scope
     */
    public static Scope getCurrentScope() {
        return scope;
    }

    /**
     * Start a new scope for variables. Any variables created after opening a
     * new scope are only valid until the scope is closed, at which time they
     * are replaced with their old values, whereas variables from outside the
     * current scope that are modified will keep their new values even after
     * closing the inner scope.
     */
    public static void openVariableScope() {
        scope = new Scope(scope);
    }

    /**
     * End a scope for variables, removing all active variables that only
     * existed in the new scope.
     */
    public static void closeVariableScope() {
        scope = scope.getParent();
    }

    /**
     * Set the value of a variable that may be in the innermost scope or an
     * outer scope. The new value will be set in the scope in which the variable
     * was defined, or in the current scope if the variable was not set.
     * 
     * @param node
     *            variable to set
     */
    public static void setVariable(Variable node) {
        scope.setVariable(node);
    }

    /**
     * Add a scope specific local variable, typically a function or mixin
     * parameter.
     * 
     * @param node
     *            variable to add
     */
    public static void addVariable(Variable node) {
        scope.addVariable(node);
    }

    public static Variable getVariable(String string) {
        return scope.getVariable(string);
    }

    public static Iterable<Variable> getVariables() {
        return scope.getVariables();
    }

    public static MixinDefNode getMixinDefinition(String name) {
        return scope.getMixinDefinition(name);
    }

    public static FunctionDefNode getFunctionDefinition(String name) {
        return scope.getFunctionDefinition(name);
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Returns the directory containing this style sheet
     * 
     * @return The directory containing this style sheet
     */
    public String getDirectory() {
        return file.getParent();
    }

    /**
     * Returns the full file name for this style sheet
     * 
     * @return The full file name for this style sheet
     */
    public String getFileName() {
        return file.getPath();
    }

    public static final void warning(String msg) {
        Logger.getLogger(ScssStylesheet.class.getName()).warning(msg);
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    private String buildString(BuildStringStrategy strategy) {
        StringBuilder string = new StringBuilder("");
        String delimeter = "\n\n";
        // add charset declaration, if it is not default "ASCII".
        if (!"ASCII".equals(getCharset())) {
            string.append("@charset \"").append(getCharset()).append("\";")
                    .append(delimeter);
        }
        List<Node> children = getChildren();
        if (children.size() > 0) {
            string.append(strategy.build(children.get(0)));
        }
        if (children.size() > 1) {
            for (int i = 1; i < children.size(); i++) {
                String childString = strategy.build(children.get(i));
                if (childString != null) {
                    string.append(delimeter).append(childString);
                }
            }
        }
        String output = string.toString();
        return output;
    }

    static {
        String logFile = System.getProperty("java.util.logging.config.file");
        if (logFile == null) {
            try {
                LogManager.getLogManager().readConfiguration(
                        ScssStylesheet.class
                                .getResourceAsStream("/logging.properties"));
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
