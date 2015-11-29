/*
 * Copyright 2015 Francesco Pontillo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.frapontillo.pulse.crowd.tag.tagme;

import com.github.frapontillo.pulse.crowd.data.entity.Message;
import com.github.frapontillo.pulse.crowd.data.entity.Tag;
import com.github.frapontillo.pulse.crowd.tag.ITaggerOperator;
import com.github.frapontillo.pulse.spi.IPlugin;
import com.github.frapontillo.pulse.spi.VoidConfig;
import com.github.frapontillo.pulse.util.PulseLogger;
import org.apache.logging.log4j.Logger;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.Observable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Francesco Pontillo
 * @see {@link "http://tagme.di.unipi.it/tagme_help.html#tagging"}
 */
public class TagMeTagger extends IPlugin<Message, Message, VoidConfig> {
    public final static String PLUGIN_NAME = "tagme";
    private final static String TAG_ME_ENDPOINT = "http://tagme.di.unipi.it";
    private final static List<String> supportedLangs = Arrays.asList("IT", "EN");
    private Logger logger = PulseLogger.getLogger(TagMeTagger.class);

    private TagMeService service;

    @Override public String getName() {
        return PLUGIN_NAME;
    }

    @Override public VoidConfig getNewParameter() {
        return new VoidConfig();
    }

    @Override protected Observable.Operator<Message, Message> getOperator(VoidConfig parameters) {
        return new ITaggerOperator(this) {
            @Override protected List<Tag> getTagsImpl(String text, String language) {
                // get the tags
                TagMeResponse response;
                List<Tag> tags = new ArrayList<>();

                if (language != null && supportedLangs.contains(language.toUpperCase())) {
                    try {
                        response = getService().tag(text, language);
                        for (TagMeResponse.TagMeAnnotation annotation : response.getAnnotations()) {
                            Tag tag = new Tag();
                            tag.setText(annotation.getTitle());
                            tag.addSource(getName());
                            tags.add(tag);
                        }
                    } catch (RetrofitError e) {
                        // ignored
                        Response res = e.getResponse();
                        if (res != null) {
                            logger.error(String.format("%s returned\n%s: %s", e.getUrl(),
                                    res.getStatus(), res.getReason()), e);
                        } else {
                            logger.error(String.format("%s returned an error", e.getUrl()), e);
                        }
                    } catch (Exception e) {
                        // ignored
                        logger.error(e);
                    }
                }
                // publish the tags as a connectable observable
                return tags;
            }
        };
    }

    private TagMeService getService() {
        if (service == null) {
            // build the REST client
            RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(TAG_ME_ENDPOINT)
                    .setRequestInterceptor(new TagMeInterceptor()).build();
            service = restAdapter.create(TagMeService.class);
        }
        return service;
    }
}