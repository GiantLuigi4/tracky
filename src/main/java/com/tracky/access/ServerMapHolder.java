package com.tracky.access;

import com.tracky.api.TrackingSource;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ServerMapHolder {

    Collection<TrackingSource> trackyTrackingSources();
}
