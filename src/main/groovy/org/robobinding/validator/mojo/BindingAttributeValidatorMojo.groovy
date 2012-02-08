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
package org.robobinding.validator.mojo

import org.apache.maven.plugin.MojoFailureException
import org.codehaus.groovy.maven.mojo.GroovyMojo
import org.robobinding.validator.BindingAttributeValidator

/**
 *
 * @goal validate-bindings
 * @phase compile
 * @configurator include-project-dependencies
 * 
 * @since 1.0
 * @version $Revision: 1.0 $
 * @author Robert Taylor
 */
class BindingAttributeValidatorMojo extends GroovyMojo
{
	/**
	 * @parameter expression="${basedir}"
	 * @required
	 */
	def baseFolder
	
	void execute()
	{
		log.info("Validating binding attributes...")
		
		def errorMessages = new BindingAttributeValidator(baseFolder).validate()
		
		if (errorMessages)
		   throw new MojoFailureException(describe(errorMessages))
		
		log.info("Done!")
	}
	
	def describe(errorMessages) {
		def message
		
		errorMessages.each {
			message += "\n\n${it}"
		}
		
		message
	}
}
