/**
 * Copyright 2012 Cheng Wei, Robert Taylor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.robobinding.plugins.validator

import groovy.lang.Closure

import org.mockito.Mockito
import org.robobinding.binder.BindingAttributeProcessor
import org.robobinding.binder.ViewNameResolver

import android.view.View

/**
 *
 * @since 1.0
 * @version $Revision: 1.0 $
 * @author Robert Taylor
 */
class BindingAttributeValidator {

	static final def LAYOUT_FOLDER = ~/[layout].*/
	static final def XML_FILE = ~/.*[.xml]/
	static final def ROBOBINDING_NAMESPACE = 'http://robobinding.org/android'

	def resFolder
	def bindingAttributeProcessor
	def fileChangeChecker
	def errorReporter
	def viewNameResolver

	BindingAttributeValidator(baseFolder,fileChangeChecker,errorReporter) {
		resFolder = new File(baseFolder, "res")
		this.fileChangeChecker = fileChangeChecker
		this.errorReporter = errorReporter
		viewNameResolver = new ViewNameResolver()
	}

	def validate() {
		def errorMessages = []

		inEachLayoutFolder { layoutFolder ->

			inEachXmlFileWithBindingsThatHasChangedInsideThe(layoutFolder) { xmlFile ->

				errorReporter.clearErrorsFor(xmlFile)
				
				forEachViewWithBindingAttributesInThe(xmlFile.text) { viewName, attributes ->

					def fullyQualifiedViewName = getFullyQualifiedViewName(viewName)
					def errorMessage = validateView(fullyQualifiedViewName, attributes)

					if (errorMessage) {
						errorMessages << "${xmlFile.name}: ${errorMessage}"
						
						int lineNumber = 1
						errorReporter.errorIn(xmlFile, lineNumber, errorMessage)
					}
				}
			}
		}

		errorMessages
	}

	def inEachLayoutFolder (Closure c) {
		resFolder.eachDirMatch(LAYOUT_FOLDER) { c.call(it) }
	}

	def inEachXmlFile(folder, Closure c) {
		folder.eachFileMatch(XML_FILE) { c.call(it) }
	}

	def inEachXmlFileWithBindingsThatHasChangedInsideThe(folder, Closure c) {
		inEachXmlFile(folder) {
			if (fileChangeChecker.hasFileChangedSinceLastBuild(it)) {
				if (getRoboBindingNamespaceDeclaration(it.text)) {
					c.call(it)
				}
			}
		}
	}

	def getRoboBindingNamespaceDeclaration(xml) {
		def rootNode = new XmlSlurper().parseText(xml)

		def xmlClass = rootNode.getClass()
		def gpathClass = xmlClass.getSuperclass()
		def namespaceTagHintsField = gpathClass.getDeclaredField("namespaceTagHints")
		namespaceTagHintsField.setAccessible(true)

		def namespaceDeclarations = namespaceTagHintsField.get(rootNode)

		return namespaceDeclarations.find { key, value ->
			value == ROBOBINDING_NAMESPACE
		}?.key
	}

	def forEachViewWithBindingAttributesInThe(xml, Closure c) {
		def rootNode = new XmlSlurper().parseText(xml)

		rootNode.children().each { processViewNode(it, c) }
	}

	def processViewNode(viewNode, Closure c) {
		def viewName = viewNode.name()
		def viewAttributes = viewNode.attributes()

		def nodeField = viewNode.getClass().getDeclaredField("node")
		nodeField.setAccessible(true)
		def node = nodeField.get(viewNode)

		def bindingAttributes = node.@attributeNamespaces.findAll { it.value == ROBOBINDING_NAMESPACE }
		def bindingAttributeNames = bindingAttributes*.key
		c.call(viewName, viewAttributes.subMap(bindingAttributeNames))

		viewNode.children().each { processViewNode(it, c) }
	}

	def getFullyQualifiedViewName(viewName) {
		viewNameResolver.getViewNameFromLayoutTag(viewName)
	}

	def validateView(String fullyQualifiedViewName, attributes) {
		if (!fullyQualifiedViewName.startsWith("android"))
			return

		def errorMessage = validateView(instanceOf(fullyQualifiedViewName), attributes)

		if (errorMessage)
			errorMessage = "${fullyQualifiedViewName} has binding errors:\n\n${errorMessage}"

		errorMessage
	}

	def validateView(View view, attributes) {
		def errorMessage = ""

		try {
			getBindingAttributeProcessor().process(view, attributes)
		}
		catch (RuntimeException e) {
			errorMessage = "${e.message}"
		}

		errorMessage
	}

	def instanceOf(fullyQualifiedViewName) {
		Class viewClass = Class.forName(fullyQualifiedViewName)
		Mockito.mock(viewClass)
	}

	def getBindingAttributeProcessor() {
		if (bindingAttributeProcessor == null)
			bindingAttributeProcessor = new BindingAttributeProcessor(null, true)

		bindingAttributeProcessor
	}
}
