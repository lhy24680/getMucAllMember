package com.lhy.mucAllMembers.dao;

import org.jivesoftware.database.DbConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* 根据房间名查询出房间中所有人员，包含在线和离线
*/
public class ChatRoomDao {
	
	public static List<Map<String, String>> getMucAllMembers(String roomId) {
		
		StringBuilder sqlBuilder = new StringBuilder();

		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		sqlBuilder.append("select ofmucaffiliation.jid, ofmucaffiliation.affiliation as affiliation from ");
		sqlBuilder.append(" ofmucroom  join ofmucaffiliation on ofmucroom.roomID = ofmucaffiliation.roomID and ofmucroom.name = ?");
		sqlBuilder.append(" union ");
		sqlBuilder.append(" select ofmucmember.jid, \"30\" as affiliation from "); // 普通成员的权限是30，见MUCRole.Affiliation.member
		sqlBuilder.append(" ofmucroom join ofmucmember on ofmucroom.roomID = ofmucmember.roomID and ofmucroom.name = ?");
		sqlBuilder.append(" order by affiliation");
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		Map<String, String> map = null;
		try {
			connection = DbConnectionManager.getConnection();
			statement = connection.prepareStatement(sqlBuilder.toString());
			statement.setString(1, roomId);
			statement.setString(2, roomId);
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				map = new HashMap<String, String>();
				map.put("jid", resultSet.getString(1));
				map.put("affiliation", resultSet.getString(2));
				list.add(map);
			}
            System.out.println("getMucAllMembers-- " + list.toArray().toString());
		} catch (Exception e1) {
			e1.printStackTrace();
		} finally {
			DbConnectionManager.closeConnection(resultSet, statement,
					connection);
		}
		return list;
	}
}
