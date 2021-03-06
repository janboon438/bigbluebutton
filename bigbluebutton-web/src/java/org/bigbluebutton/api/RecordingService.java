package org.bigbluebutton.api;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

import org.bigbluebutton.api.domain.Recording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordingService {
	private static Logger log = LoggerFactory.getLogger(RecordingService.class);
	
	private String publishedDir = "/var/bigbluebutton/published";
	private String unpublishedDir = "/var/bigbluebutton/unpublished";
	private RecordingServiceHelper recordingServiceHelper;
	private String recordStatusDir;
	
	public void startIngestAndProcessing(String meetingId) {	
		String done = recordStatusDir + "/" + meetingId + ".done";
	
		File doneFile = new File(done);
		if (!doneFile.exists()) {
			try {
				doneFile.createNewFile();
				if (!doneFile.exists())
					log.error("Failed to create " + done + " file.");
			} catch (IOException e) {
				log.error("Failed to create " + done + " file.");
			}			
		} else {
			log.error(done + " file already exists.");
		}
	}
	
	public ArrayList<Recording> getRecordings(String meetingId) {
		ArrayList<Recording> recs = new ArrayList<Recording>();
		
		ArrayList<Recording> published = getRecordingsForPath(meetingId, publishedDir);
		if (!published.isEmpty()) {
			recs.addAll(published);
		}
		
		ArrayList<Recording> unpublished = getRecordingsForPath(meetingId, unpublishedDir);
		if (!unpublished.isEmpty()) {
			recs.addAll(unpublished);
		}
		
		return recs;
	}
	
	private ArrayList<Recording> getRecordingsForPath(String meetingId, String path) {
		ArrayList<Recording> recs = new ArrayList<Recording>();
		
		String[] format = getPlaybackFormats(path);
		for (int i = 0; i < format.length; i++) {
			File[] recordings = getDirectories(path + File.pathSeparatorChar + format[i]);
			for (int f = 0; f < recordings.length; f++) {
				if (recordings[f].getName().startsWith(meetingId)) {
					Recording r = getRecordingInfo(path, recordings[f].getName(), format[i]);
					if (r != null) recs.add(r);
				}				
			}
		}			
		return recs;
	}
	
	public Recording getRecordingInfo(String recordingId, String format) {
		return getRecordingInfo(publishedDir, recordingId, format);
	}

	private Recording getRecordingInfo(String path, String recordingId, String format) {
		Recording rec = recordingServiceHelper.getRecordingInfo(recordingId, path, format);
		return rec;
	}
	
	public void publish(String recordingId, boolean publish) {
		publish(publishedDir, recordingId, publish);
		publish(unpublishedDir, recordingId, publish);
	}
	
	private void publish(String path, String recordingId, boolean publish) {
		String[] format = getPlaybackFormats(path);
		for (int i = 0; i < format.length; i++) {
			File[] recordings = getDirectories(path + File.pathSeparatorChar + format[i]);
			for (int f = 0; f < recordings.length; f++) {
				if (recordings[f].getName().equals(recordingId)) {
					Recording r = getRecordingInfo(recordingId, path, format[i]);
					if (r != null) {
						File dest;
						if (publish) {
							dest = new File(publishedDir);
						} else {
							dest = new File(unpublishedDir);
						}
						boolean moved = recordings[f].renameTo(new File(dest, recordings[f].getName()));
						if (moved) {
							r.setPublished(publish);
							recordingServiceHelper.writeRecordingInfo(dest.getAbsolutePath() + File.pathSeparatorChar + recordings[f].getName(), r);
						}
					}
				}				
			}
		}
	}
		
	public void delete(String recordingId) {
		deleteRecording(recordingId, publishedDir);
		deleteRecording(recordingId, unpublishedDir);
	}
	
	private void deleteRecording(String id, String path) {
		String[] format = getPlaybackFormats(path);
		for (int i = 0; i < format.length; i++) {
			File[] recordings = getDirectories(path + File.pathSeparatorChar + format[i]);
			for (int f = 0; f < recordings.length; f++) {
				if (recordings[f].getName().equals(id)) {
					deleteDirectory(recordings[f]);
				}				
			}
		}		
	}
	
	private void deleteDirectory(File directory) {
		/**
		 * Go through each directory and check if it's not empty.
		 * We need to delete files inside a directory before a
		 * directory can be deleted.
		**/
		File[] files = directory.listFiles();				
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				deleteDirectory(files[i]);
			} else {
				files[i].delete();
			}
		}
		// Now that the directory is empty. Delete it.
		directory.delete();	
	}
	
	private File[] getDirectories(String path) {
		File dir = new File(path);
		FileFilter fileFilter = new FileFilter() {
		    public boolean accept(File file) {
		        return file.isDirectory();
		    }
		};		
		return dir.listFiles(fileFilter);		
	}
	
	private String[] getPlaybackFormats(String path) {
		File[] dirs = getDirectories(path);
		String[] formats = new String[dirs.length];
		
		for (int i = 0; i < dirs.length; i++) {
			formats[i] = dirs[i].getName();
		}
		return formats;
	}
	
	public void setRecordingStatusDir(String dir) {
		recordStatusDir = dir;
	}
	
	public void setUnpublishedDir(String dir) {
		unpublishedDir = dir;
	}
	
	public void setPublishedDir(String dir) {
		publishedDir = dir;
	}
	
	public void setRecordingServiceHelper(RecordingServiceHelper r) {
		recordingServiceHelper = r;
	}
}
