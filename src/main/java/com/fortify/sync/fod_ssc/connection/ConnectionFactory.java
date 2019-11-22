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
package com.fortify.sync.fod_ssc.connection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.connection.fod.FoDConnectionFactory;
import com.fortify.sync.fod_ssc.connection.fod.IConfigFoDCredentials;
import com.fortify.sync.fod_ssc.connection.ssc.IConfigSSCCredentials;
import com.fortify.sync.fod_ssc.connection.ssc.SSCConnectionFactory;

@Component
public final class ConnectionFactory {
	private final FoDConnectionFactory fodConnFactory;
	private final SSCConnectionFactory sscConnFactory;
	
	@Autowired
	public ConnectionFactory(FoDConnectionFactory fodConnFactory, SSCConnectionFactory sscConnFactory) {
		this.fodConnFactory = fodConnFactory;
		this.sscConnFactory = sscConnFactory;
	}
	
	public final FoDAuthenticatingRestConnection getFodConnection(IConfigFoDCredentials credentials) {
		return fodConnFactory.getFoDConnection(credentials);
	}
	
	public final SSCAuthenticatingRestConnection getSSCConnection(IConfigSSCCredentials credentials) {
		return sscConnFactory.getSSCConnection(credentials);
	}

}
