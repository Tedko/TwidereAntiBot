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

import org.mariotaku.microblog.library.api.statusnet.TimelineResources;
import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.param.Query;
import org.mariotaku.microblog.library.api.statusnet.GroupResources;
import org.mariotaku.microblog.library.api.statusnet.SearchResources;
import org.mariotaku.microblog.library.api.statusnet.StatusNetResources;
import org.mariotaku.microblog.library.api.statusnet.UserResources;
import org.mariotaku.microblog.library.model.microblog.User;

/**
 * Created by mariotaku on 16/3/4.
 */
public interface StatusNet extends StatusNetResources, GroupResources, SearchResources,
        UserResources, TimelineResources {

    @GET("/externalprofile/show.json")
    User showExternalProfile(@Query("profileurl") String profileUrl) throws MicroBlogException;

}
