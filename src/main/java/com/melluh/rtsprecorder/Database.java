package com.melluh.rtsprecorder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.melluh.rtsprecorder.util.FormatUtil;

public class Database {

	private Connection conn;
	
	public void connect() {
		try {
			this.conn = DriverManager.getConnection("jdbc:sqlite:data.db");
			this.createTables();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
	
	private void createTables() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS recordings (file_path VARCHAR(50) NOT NULL PRIMARY KEY, camera_name VARCHAR(50) NOT NULL, start_time DATETIME NOT NULL, end_time DATETIME NOT NULL)");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS clips (file_path VARCHAR(50) NOT NULL PRIMARY KEY, label VARCHAR(200) NOT NULL, camera_name VARCHAR(50) NOT NULL, start_time DATETIME NOT NULL, end_time DATETIME NOT NULL, saved_time DATETIME NOT NULL)");
	}
	
	public void storeRecording(Recording recording) {
		try {
			PreparedStatement prepStmt = conn.prepareStatement("INSERT INTO recordings (file_path, camera_name, start_time, end_time) VALUES (?, ?, ?, ?)");
			prepStmt.setString(1, recording.getFilePath());
			prepStmt.setString(2, recording.getCameraName());
			prepStmt.setString(3, recording.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			prepStmt.setString(4, recording.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			prepStmt.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
	
	public List<Recording> getRecordings(LocalDate date, String cameraName) {
		try {
			PreparedStatement prepStmt = conn.prepareStatement("SELECT * FROM recordings WHERE date(start_time) = ? AND camera_name = ? ORDER BY start_time ASC");
			prepStmt.setString(1, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
			prepStmt.setString(2, cameraName);
			
			ResultSet result = prepStmt.executeQuery();
			return this.getRecordingsList(result);
		} catch (SQLException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public List<Recording> getRecordings(LocalDateTime from, LocalDateTime to, String cameraName) {
		String fromStr = from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		String toStr = to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

		try {
			PreparedStatement prepStmt = conn.prepareStatement("SELECT * FROM recordings WHERE camera_name = ? AND ((start_time > ? AND end_time < ?) OR (start_time < ? AND end_time > ?) OR (start_time < ? AND end_time > ?)) ORDER BY start_time ASC");
			prepStmt.setString(1, cameraName);
			prepStmt.setString(2, fromStr);
			prepStmt.setString(3, toStr);
			prepStmt.setString(4, fromStr);
			prepStmt.setString(5, fromStr);
			prepStmt.setString(6, toStr);
			prepStmt.setString(7, toStr);

			ResultSet resultSet = prepStmt.executeQuery();
			return this.getRecordingsList(resultSet);
		} catch (SQLException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public List<Recording> getOldestRecordings(int num) {
		try {
			ResultSet result = conn.createStatement().executeQuery("SELECT * FROM recordings ORDER BY start_time ASC LIMIT " + num);
			return this.getRecordingsList(result);
		} catch (SQLException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	private List<Recording> getRecordingsList(ResultSet result) throws SQLException {
		List<Recording> recordings = new ArrayList<>();
		while(result.next()) {
			LocalDateTime startTime = FormatUtil.parseDateTime(result.getString("start_time"));
			LocalDateTime endTime = FormatUtil.parseDateTime(result.getString("end_time"));
			recordings.add(new Recording(result.getString("file_path"), result.getString("camera_name"), startTime, endTime));
		}
		return recordings;
	}
	
	public void removeRecording(String filePath) {
		try {
			PreparedStatement prepStmt = conn.prepareStatement("DELETE FROM recordings WHERE file_path = ?");
			prepStmt.setString(1, filePath);
			prepStmt.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public void storeClip(Clip clip) {
		try {
			PreparedStatement prepStmt = conn.prepareStatement("INSERT INTO clips (file_path, label, camera_name, start_time, end_time, saved_time) VALUES (?, ?, ?, ?, ?, ?)");
			prepStmt.setString(1, clip.getFilePath());
			prepStmt.setString(2, clip.getLabel());
			prepStmt.setString(3, clip.getCameraName());
			prepStmt.setString(4, clip.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			prepStmt.setString(5, clip.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			prepStmt.setString(6, clip.getSavedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			prepStmt.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public List<Clip> getClips() {
		try {
			ResultSet result = conn.createStatement().executeQuery("SELECT * FROM clips");
			List<Clip> clips = new ArrayList<>();

			while(result.next()) {
				clips.add(new Clip(
						result.getString("file_path"),
						result.getString("label"),
						result.getString("camera_name"),
						FormatUtil.parseDateTime(result.getString("start_time")),
						FormatUtil.parseDateTime(result.getString("end_time")),
						FormatUtil.parseDateTime(result.getString("saved_time"))
				));
			}

			return clips;
		} catch (SQLException ex) {
			ex.printStackTrace();
			return Collections.emptyList();
		}
	}
	
	public Connection getConnection() {
		return conn;
	}
	
}
