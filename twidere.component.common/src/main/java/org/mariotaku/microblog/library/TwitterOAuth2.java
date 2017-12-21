/*
 *         Twidere - Twitter client for Android
 *
 * Copyright 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mariotaku.microblog.library;

import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.param.Header;
import org.mariotaku.restfu.annotation.param.KeyValue;
import org.mariotaku.restfu.annotation.param.Params;
import org.mariotaku.microblog.library.auth.OAuth2GetTokenHeader;
import org.mariotaku.microblog.library.auth.OAuth2Token;

/**
 * Created by mariotaku on 16/1/4.
 */
public interface TwitterOAuth2 {

    @POST("/oauth2/token")
    @Params(@KeyValue(key = "grant_type", value = "client_credentials"))
    OAuth2Token getApplicationOnlyAccessToken(@Header("Authorization") OAuth2GetTokenHeader token)
            throws MicroBlogException;
}
