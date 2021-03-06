/*
 * Copyright 2010 Nabeel Mukhtar 
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
 * 
 */
package com.github.api.v2.services.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.api.v2.schema.Discussion;
import com.github.api.v2.schema.Gist;
import com.github.api.v2.schema.IntegerPayloadPullRequest;
import com.github.api.v2.schema.Issue;
import com.github.api.v2.schema.Job;
import com.github.api.v2.schema.Language;
import com.github.api.v2.schema.Member;
import com.github.api.v2.schema.ObjectPayloadPullRequest;
import com.github.api.v2.schema.ObjectPayloadTarget;
import com.github.api.v2.schema.Organization;
import com.github.api.v2.schema.PayloadPullRequest;
import com.github.api.v2.schema.PayloadTarget;
import com.github.api.v2.schema.Permission;
import com.github.api.v2.schema.Repository;
import com.github.api.v2.schema.SchemaEntity;
import com.github.api.v2.schema.StringMember;
import com.github.api.v2.schema.StringPayloadTarget;
import com.github.api.v2.schema.Tree;
import com.github.api.v2.schema.User;
import com.github.api.v2.schema.UserFeed;
import com.github.api.v2.services.AsyncResponseHandler;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubService;
import com.github.api.v2.services.constant.ApplicationConstants;
import com.github.api.v2.services.constant.GitHubApiUrls.GitHubApiUrlBuilder;
import com.google.mygson.gh4a.FieldNamingPolicy;
import com.google.mygson.gh4a.Gson;
import com.google.mygson.gh4a.GsonBuilder;
import com.google.mygson.gh4a.JsonArray;
import com.google.mygson.gh4a.JsonDeserializationContext;
import com.google.mygson.gh4a.JsonDeserializer;
import com.google.mygson.gh4a.JsonElement;
import com.google.mygson.gh4a.JsonObject;
import com.google.mygson.gh4a.JsonParseException;
import com.google.mygson.gh4a.JsonParser;
import com.google.mygson.gh4a.reflect.TypeToken;

/**
 * The Class BaseGitHubService.
 */
public abstract class BaseGitHubService extends GitHubApiGateway implements GitHubService {
	
	/** The Constant UTF_8_CHAR_SET. */
	protected static final Charset UTF_8_CHAR_SET = Charset.forName(ApplicationConstants.CONTENT_ENCODING);
	
    /** The parser. */
    protected final JsonParser parser = new JsonParser();
    
    /** The handlers. */
    private List<AsyncResponseHandler<List<? extends SchemaEntity>>> handlers = new ArrayList<AsyncResponseHandler<List<? extends SchemaEntity>>>();
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT);
    
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                                                       
    private static final SimpleDateFormat sdf3 = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'UTC' yyyy");//not sure why it fails in android when using EEE MMM dd HH:mm:ss z yyyy
    
	/**
	 * Instantiates a new base git hub service.
	 */
	public BaseGitHubService() {
        // by default we compress contents
        requestHeaders.put("Accept-Encoding", "gzip, deflate");
	}

	/**
	 * Instantiates a new base git hub service.
	 * 
	 * @param apiVersion
	 *            the api version
	 */
	public BaseGitHubService(String apiVersion) {
		setApiVersion(apiVersion);
	}
	
	/**
	 * Unmarshall.
	 * 
	 * @param typeToken
	 *            the type token
	 * @param response
	 *            the response
	 * 
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	protected <T> T unmarshall(TypeToken<T> typeToken, JsonElement response) throws JsonParseException {
		Gson gson = getGsonBuilder().create();
		return (T) gson.fromJson(response, typeToken.getType());
	}

	/**
	 * Notify observers.
	 * 
	 * @param response
	 *            the response
	 */
	protected void notifyObservers(List<? extends SchemaEntity> response) {
		for(AsyncResponseHandler<List<? extends SchemaEntity>> handler : handlers) {
			handler.handleResponse(response);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.stackexchange.client.query.StackExchangeApiQuery#addResonseHandler(com.google.code.stackexchange.client.AsyncResponseHandler)
	 */
	/**
	 * Adds the resonse handler.
	 * 
	 * @param handler
	 *            the handler
	 */
	public void addResonseHandler(AsyncResponseHandler<List<? extends SchemaEntity>> handler) {
		handlers.add(handler);
	}
	
	/**
	 * Gets the gson builder.
	 * 
	 * @return the gson builder
	 */
	protected GsonBuilder getGsonBuilder() {
		GsonBuilder builder = new GsonBuilder();
		builder.setDateFormat(ApplicationConstants.DATE_FORMAT);
		builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

		//added by slapperwan
		builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {

            @Override
            public Date deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
                
                try {
                    return sdf.parse(arg0.getAsJsonPrimitive().getAsString());
                }
                catch (ParseException e) {
                    //sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    try {
                        return sdf2.parse(arg0.getAsJsonPrimitive().getAsString());
                    }
                    catch (ParseException e1) {
                        try {
                            return sdf3.parse(arg0.getAsJsonPrimitive().getAsString());
                        }
                        catch (ParseException e2) {
                            return null;
                        }
                    }
                }
            }
		    
        });
		builder.registerTypeAdapter(PayloadPullRequest.class, new PayloadPullRequestDeserializer());
		
		builder.registerTypeAdapter(Member.class, new MemberDeserializer());
		
		builder.registerTypeAdapter(PayloadTarget.class, new PayloadTargetDeserializer());
		
		builder.registerTypeAdapter(Issue.State.class, new JsonDeserializer<Issue.State>() {
			@Override
			public Issue.State deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return Issue.State.fromValue(arg0.getAsString());
			}
		});
		builder.registerTypeAdapter(Repository.Visibility.class, new JsonDeserializer<Repository.Visibility>() {
			@Override
			public Repository.Visibility deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return (arg0.getAsBoolean())? Repository.Visibility.PRIVATE : Repository.Visibility.PUBLIC;
			}
		});
		builder.registerTypeAdapter(Gist.Visibility.class, new JsonDeserializer<Gist.Visibility>() {
			@Override
			public Gist.Visibility deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return (arg0.getAsBoolean())? Gist.Visibility.PUBLIC : Gist.Visibility.PRIVATE;
			}
		});
		builder.registerTypeAdapter(Language.class, new JsonDeserializer<Language>() {
			@Override
			public Language deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return Language.fromValue(arg0.getAsString());
			}
		});
		builder.registerTypeAdapter(Tree.Type.class, new JsonDeserializer<Tree.Type>() {
			@Override
			public Tree.Type deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return Tree.Type.fromValue(arg0.getAsString());
			}
		});
		builder.registerTypeAdapter(Organization.Type.class, new JsonDeserializer<Organization.Type>() {
			@Override
			public Organization.Type deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return Organization.Type.fromValue(arg0.getAsString());
			}
		});
		builder.registerTypeAdapter(Discussion.Type.class, new JsonDeserializer<Discussion.Type>() {
			@Override
			public Discussion.Type deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return Discussion.Type.fromValue(arg0.getAsString());
			}
		});
		builder.registerTypeAdapter(Permission.class, new JsonDeserializer<Permission>() {
			@Override
			public Permission deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return Permission.fromValue(arg0.getAsString());
			}
		});
		builder.registerTypeAdapter(UserFeed.Type.class, new JsonDeserializer<UserFeed.Type>() {
            @Override
            public UserFeed.Type deserialize(JsonElement arg0, Type arg1,
                    JsonDeserializationContext arg2) throws JsonParseException {
                return UserFeed.Type.fromValue(arg0.getAsString());
            }
        });
		builder.registerTypeAdapter(Job.Type.class, new JsonDeserializer<Job.Type>() {
			@Override
			public Job.Type deserialize(JsonElement arg0, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				return Job.Type.fromValue(arg0.getAsString());
			}
		});
		return builder;
	}
    
	/**
	 * Unmarshall.
	 * 
	 * @param jsonContent
	 *            the json content
	 * 
	 * @return the json object
	 */
	protected JsonObject unmarshall(InputStream jsonContent) {
        try {
        	JsonElement element = parser.parse(new InputStreamReader(jsonContent, UTF_8_CHAR_SET));
        	if (element.isJsonObject()) {
        		return element.getAsJsonObject();
        	} else {
        		throw new GitHubException("Unknown content found in response." + element);
        	}
        } catch (Exception e) {
            throw new GitHubException(e);
        } finally {
	        closeStream(jsonContent);
	    }
	}
	
	/**
     * Unmarshall.
     * 
     * @param jsonContent
     *            the json content
     * 
     * @return the json array
     */
    protected JsonArray unmarshallArray(InputStream jsonContent) {
        try {
            JsonElement element = parser.parse(new InputStreamReader(jsonContent, UTF_8_CHAR_SET));
            if (element.isJsonArray()) {
                return element.getAsJsonArray();
            } else {
                throw new GitHubException("Unknown content found in response." + element);
            }
        } catch (Exception e) {
            throw new GitHubException(e);
        } finally {
            closeStream(jsonContent);
        }
    }
	
	/**
	 * Creates the git hub api url builder.
	 * 
	 * @param urlFormat
	 *            the url format
	 * 
	 * @return the git hub api url builder
	 */
	protected GitHubApiUrlBuilder createGitHubApiUrlBuilder(String urlFormat) {
		return new GitHubApiUrlBuilder(urlFormat);
	}
	
	private class PayloadPullRequestDeserializer implements JsonDeserializer<PayloadPullRequest> {
	    public PayloadPullRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
	        throws JsonParseException {
	        if (json.isJsonPrimitive()) {
	            IntegerPayloadPullRequest payloadPullRequest = new IntegerPayloadPullRequest();
	            payloadPullRequest.setNumber(json.getAsInt());
	            return payloadPullRequest;
	        }
	        else if (json.isJsonObject()) {
	            ObjectPayloadPullRequest obj2 = context.deserialize(json, new TypeToken<ObjectPayloadPullRequest>(){}.getType());
	            return obj2;
	        }
	        return null;
	    }
	}
	
	private class MemberDeserializer implements JsonDeserializer<Member> {
        public Member deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
            if (json.isJsonPrimitive()) {
                StringMember member = new StringMember();
                member.setLogin(json.getAsString());
                return member;
            }
            else if (json.isJsonObject()) {
                User member = context.deserialize(json, new TypeToken<User>(){}.getType());
                return member;
            }
            return null;
        }
    }
	
	private class PayloadTargetDeserializer implements JsonDeserializer<PayloadTarget> {
        public PayloadTarget deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
            if (json.isJsonPrimitive()) {
                StringPayloadTarget payloadTarget = new StringPayloadTarget();
                payloadTarget.setLogin(json.getAsString());
                return payloadTarget;
            }
            else if (json.isJsonObject()) {
                ObjectPayloadTarget obj2 = context.deserialize(json, new TypeToken<ObjectPayloadTarget>(){}.getType());
                return obj2;
            }
            return null;
        }
    }
}
