/*
 * Copyright 2013 SIB Visions GmbH
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
 *
 *
 * History
 *
 * 01.01.2013 - [JR] - creation
 */
package com.sibvisions.vertx;

import java.util.Hashtable;

/**
 * The <code>Util</code> is a utility class for dealing with vert.x.
 * 
 * @author René Jahn
 */
final class Util
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Invisible constructor because <code>Util</code> is a utility
	 * class.
	 */
	private Util()
	{
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// User-defined methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Gets all available parameters from passed command-line arguments.
	 * 
	 * @param pArgs th command-line arguments
	 * @return the application parameters
	 */
	public static Hashtable<String, String> parseCommandLineProperties(String[] pArgs)
	{
		Hashtable<String, String> htParams = new Hashtable<String, String>();
		
		if (pArgs != null)
		{
			String[] sValue;
			
			int iPos;
			
			for (int i = 0; i < pArgs.length; i++)
			{
				if (pArgs[i].startsWith("-"))
				{
					iPos = pArgs[i].indexOf("=");
					
					sValue = new String[2];

					if (iPos > 0)
					{
						sValue[0] = pArgs[i].substring(1, iPos);
						sValue[1] = pArgs[i].substring(iPos + 1);
						
						if ((sValue[1].startsWith("\"") && sValue[1].endsWith("\""))
							|| (sValue[1].startsWith("\'") && sValue[1].endsWith("\'"))) 
						{
							sValue[1] = sValue[1].substring(1, sValue[1].length() - 1);
						}
					}
					else
					{
						sValue[0] = pArgs[i].substring(1);
						sValue[1] = "";
					}
					
					if (sValue.length == 2)
					{
						htParams.put(sValue[0], sValue[1]);
					}
				}
			}
		}
		
		return htParams;
	}
	
}	// Util
