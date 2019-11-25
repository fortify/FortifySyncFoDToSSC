/*******************************************************************************
 * (c) Copyright 2017 EntIT Software LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.sync.fod_ssc.config;

import lombok.Data;

@Data
public class ConfigLinkReleasesTask {
	private boolean enabled = false;
	private String schedule = "0 0 1 * * *"; // Currently not used, as task references property directly
	private ConfigJobLinkReleasesFoD fod = new ConfigJobLinkReleasesFoD();
	private ConfigJobLinkReleasesSSC ssc = new ConfigJobLinkReleasesSSC();
	
    @Data public static class ConfigJobLinkReleasesFoD {
    	private ConfigFoDFilters filters = new ConfigFoDFilters(); 
    }
    
    @Data public static class ConfigJobLinkReleasesSSC {
    	private boolean autoCreateVersions;
    }
    
    @Data public static class ConfigFoDFilters {
    	private ConfigFoDApplicationFilters application = new ConfigFoDApplicationFilters();
    	private ConfigFoDReleaseFilters release = new ConfigFoDReleaseFilters();
    } 
    
    @Data public static class ConfigFoDReleaseFilters {
    	private String test;
    } 
    
    @Data public static class ConfigFoDApplicationFilters {
    	private String test;
    } 
}