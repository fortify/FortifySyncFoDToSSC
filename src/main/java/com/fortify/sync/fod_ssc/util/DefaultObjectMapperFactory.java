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
package com.fortify.sync.fod_ssc.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultObjectMapperFactory {
	private static final ObjectMapper DEFAULT_OBJECT_MAPPER = _getDefaultObjectMapper();
	
	public static final ObjectMapper getDefaultObjectMapper() {
		return DEFAULT_OBJECT_MAPPER;
	}
	
	/**
	 * <p>This method returns a default {@link ObjectMapper} instance used for
	 * mapping JSON data to Java objects/values. This is mostly a default
	 * {@link ObjectMapper} configuration, apart from the following:</p>
	 * 
	 * <ul>
	 *  <li>Configured to not fail on missing properties</li>
	 * </ul>
	 */
	private static final ObjectMapper _getDefaultObjectMapper() {
		return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	/*
	private ConversionService getConversionService() {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		DefaultConversionService result = new DefaultConversionService();
		result.addConverter(new Converter<String, Date>() {
			@Override
			public Date convert(String source) {
				try {
					return format.parse(source);
				} catch ( ParseException e ) {
					throw new RuntimeException("Error parsing date format pattern", e);
				}
			}
		});
		result.addConverter(new Converter<Date, String>() {
			@Override
			public String convert(Date source) {
				return format.format(source);
			}
		});
		return result;
	}
	*/
}
