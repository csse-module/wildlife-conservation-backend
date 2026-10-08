package com.wildlife.wildlife_conservationbackend.utility;

public interface MediaStorage {
    String store(String checksum, byte[] content, String contentType);
    byte[] read(String storageKey);
}
