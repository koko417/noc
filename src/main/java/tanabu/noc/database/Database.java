package tanabu.noc.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import tanabu.noc.model.BaseInfo;
import tanabu.noc.model.BaseInfo.*;

public class Database {
	public Database() throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		initDb();

	}

	private void initDb() {
		String url = "jdbc:sqlite:test.db";

		try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user (" + "userId TEXT PRIMARY KEY," + "name TEXT,"
					+ "photoUrl TEXT" + ")");
			// Comment テーブル
			//TODO: Fix missing order the userId from relatedId
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS comment (" + "id TEXT PRIMARY KEY," + "userId TEXT,"
					+ "created INTEGER," + "updated INTEGER," + "sent INTEGER," + "relatedId TEXT," + "title TEXT,"
					+ "type INTEGER," + "raw_content TEXT," + "html_content TEXT" + ")");

			// FileInfo テーブル
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS fileinfo (" + "attachmentId TEXT," + "name TEXT,"
					+ "driveId TEXT PRIMARY KEY," + "mime TEXT," + "url TEXT," + "thumbnailUrl TEXT" + ")");

			// Task テーブル
			stmt.executeUpdate(
					"CREATE TABLE IF NOT EXISTS task (" + "id TEXT PRIMARY KEY," + "userId TEXT," + "created INTEGER,"
							+ "updated INTEGER," + "sent INTEGER," + "relatedId TEXT," + "title TEXT," + "type INTEGER,"
							+ "raw_content TEXT," + "html_content TEXT," + "maxPoints INTEGER," + "graded INTEGER,"
							+ "driveId TEXT," + "driveUrl TEXT," + "statusCode INTEGER," + "subStatus INTEGER" + ")");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static List<Field> getAllFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();
		while (clazz != null) {
			fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
			clazz = clazz.getSuperclass();
		}
		return fields;
	}

	public <T extends BaseInfo> void saveToDb(T obj) {
		Class<?> clazz = obj.getClass();

		if (!clazz.isAnnotationPresent(Table.class))
			return;
		Table table = clazz.getAnnotation(Table.class);
		StringBuilder sql = new StringBuilder("INSERT OR REPLACE INTO " + table.name() + "(");

		List<Field> fields = new ArrayList<>();
		List<String> columnNames = new ArrayList<>();

		for (Field field : getAllFields(clazz)) {
			if (field.isAnnotationPresent(Column.class)) {
				Column col = field.getAnnotation(Column.class);
				fields.add(field);
				columnNames.add(col.name());
			}
		}

		sql.append(String.join(",", columnNames));
		sql.append(") VALUES(");
		sql.append(String.join(",", Collections.nCopies(columnNames.size(), "?")));
		sql.append(")");

		try (Connection conn = DriverManager.getConnection("jdbc:sqlite:test.db?busy_timeout=5000");
				PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

			for (int i = 0; i < fields.size(); i++) {
				Field f = fields.get(i);
				f.setAccessible(true);
				Object val = f.get(obj);

				if (val == null) {
					pstmt.setNull(i + 1, Types.NULL);
				} else if (val instanceof Integer) {
					pstmt.setInt(i + 1, (Integer) val);
				} else if (val instanceof Long) {
					pstmt.setLong(i + 1, (Long) val);
				} else if (val instanceof Boolean) {
					pstmt.setInt(i + 1, ((Boolean) val) ? 1 : 0);
				} else if (val instanceof Float) {
					pstmt.setFloat(i + 1, (Float) val);
				} else if (val instanceof Double) {
					pstmt.setDouble(i + 1, (Double) val);

				} else {
					pstmt.setString(i + 1, val.toString());
				}
			}

			pstmt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
