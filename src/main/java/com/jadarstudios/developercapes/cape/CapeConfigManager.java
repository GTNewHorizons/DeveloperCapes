/**
 * DeveloperCapes by Jadar
 * License: MIT License
 * (https://raw.github.com/jadar/DeveloperCapes/master/LICENSE)
 * version 4.0.0.x
 */
package com.jadarstudios.developercapes.cape;

import com.google.common.collect.HashBiMap;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedBytes;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jadarstudios.developercapes.DevCapes;
import com.jadarstudios.developercapes.user.Group;
import com.jadarstudios.developercapes.user.GroupManager;
import com.jadarstudios.developercapes.user.User;
import com.jadarstudios.developercapes.user.UserManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Map;

/**
 * All configs need a manager, this is it.
 * 
 * @author jadar
 */
public enum CapeConfigManager {
    INSTANCE;

    protected static BitSet availableIds = new BitSet(256);
    protected HashBiMap<Integer, CapeConfig> configs;

    static {
        availableIds.clear(availableIds.size());
    }

    private CapeConfigManager() {
        configs = HashBiMap.create();
    }

    public void addConfig(int id, CapeConfig config) {
        int realId = claimId(id);
        this.configs.put(realId, config);
        addUsers(config.users);
        addGroups(config.groups);
    }
    
    private void addUsers(Map<String, User> users){
    	try {
            for (User u : users.values()) {
                UserManager.INSTANCE.addUser(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void addGroups(Map<String, Group> groups){
    	try {
            for (Group g : groups.values()) {
                GroupManager.INSTANCE.addGroup(g);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CapeConfig getConfig(int id) {
        return this.configs.get(id);
    }

    public int getIdForConfig(CapeConfig config) {
        return this.configs.inverse().get(config).intValue();
    }

    public static int getUniqueId() {
        return availableIds.nextClearBit(0);
    }

    public static int claimId(int id) {
    	if(id <= 0){
    		DevCapes.logger.error("The config ID can NOT be negative or 0!");
    		return id;
    	}
        try {
            UnsignedBytes.checkedCast(id);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        boolean isRegistered = availableIds.get(id);
        if (isRegistered) {
            DevCapes.logger.error(String.format("The config ID %d is already claimed.", id));
        }

        availableIds.set(id);
        return id;
    }

    public CapeConfig parse(String config) {
        CapeConfig instance = new CapeConfig();

        try {
            Map<String, Object> entries = new Gson().fromJson(config, Map.class);

            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                final String nodeName = entry.getKey();
                final Object obj = entry.getValue();
                if (obj instanceof Map) {
                    parseGroup(instance, nodeName, (Map) obj);
                } else if (obj instanceof String) {
                	parseUser(instance, nodeName, (String) obj);
                }
            }
        } catch (JsonSyntaxException e) {
        	DevCapes.logger.error("CapeConfig could not be parsed because:");
            e.printStackTrace();
        }

        return instance;
    }
    
    private void parseGroup(CapeConfig config, String node, Map group){
        Group g = GroupManager.INSTANCE.parse(node, group);
        if (g != null) {
        	config.groups.put(g.name, g);
        }
    }
    
    private void parseUser(CapeConfig config, String node, String user){
    	User u = UserManager.INSTANCE.parse(node, user);
        if (u != null) {
        	config.users.put(node, u);
        }
    }

    public CapeConfig parseFromStream(InputStream is) {
    	CapeConfig instance = null;
    	if (is != null) {
    		try {
    			String json = new String(ByteStreams.toByteArray(is));
    			instance = INSTANCE.parse(json);
    		} catch (IOException e) {
    			DevCapes.logger.error("Failed to read the input stream!");
    			e.printStackTrace();
    		} finally {
    			try {
    				is.close();
    			} catch (IOException e) {
    				// Ignoring this
    			}
    		}
    	} else {
    		DevCapes.logger.error("Can't parse a null input stream!");
    	}
        return instance;
    }
}