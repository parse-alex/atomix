/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import io.atomix.storage.buffer.Buffer;
import io.atomix.storage.buffer.Bytes;
import io.atomix.storage.buffer.HeapBuffer;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.atomix.utils.serializer.Serializer;

import java.nio.BufferUnderflowException;
import java.util.NoSuchElementException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Log segment reader.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class JournalSegmentReader<E> implements JournalReader<E> {
  private final Buffer buffer;
  private final int maxEntrySize;
  private final JournalSegmentCache cache;
  private final JournalIndex index;
  private final Serializer serializer;
  private final Buffer memory = HeapBuffer.allocate().flip();
  private final long firstIndex;
  private Indexed<E> currentEntry;
  private Indexed<E> nextEntry;

  public JournalSegmentReader(
      JournalSegmentDescriptor descriptor,
      int maxEntrySize,
      JournalSegmentCache cache,
      JournalIndex index,
      Serializer serializer) {
    this.buffer = descriptor.buffer().slice().duplicate();
    this.maxEntrySize = maxEntrySize;
    this.cache = cache;
    this.index = index;
    this.serializer = serializer;
    this.firstIndex = descriptor.index();
    readNext();
  }

  @Override
  public long getCurrentIndex() {
    return currentEntry != null ? currentEntry.index() : 0;
  }

  @Override
  public Indexed<E> getCurrentEntry() {
    return currentEntry;
  }

  @Override
  public long getNextIndex() {
    return currentEntry != null ? currentEntry.index() + 1 : firstIndex;
  }

  @Override
  public void reset(long index) {
    reset();
    Position position = this.index.lookup(index - 1);
    if (position != null) {
      currentEntry = new Indexed<>(position.index() - 1, null, 0);
      buffer.position(position.position());
      memory.clear().flip();
      readNext();
    }
    while (getNextIndex() < index && hasNext()) {
      next();
    }
  }

  @Override
  public void reset() {
    buffer.clear();
    memory.clear().flip();
    currentEntry = null;
    nextEntry = null;
    readNext();
  }

  @Override
  public boolean hasNext() {
    // If the next entry is null, check whether a next entry exists.
    if (nextEntry == null) {
      readNext();
    }
    return nextEntry != null;
  }

  @Override
  public Indexed<E> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    // Set the current entry to the next entry.
    currentEntry = nextEntry;

    // Reset the next entry to null.
    nextEntry = null;

    // Read the next entry in the segment.
    readNext();

    // Return the current entry.
    return currentEntry;
  }

  /**
   * Reads the next entry in the segment.
   */
  @SuppressWarnings("unchecked")
  private void readNext() {
    // Compute the index of the next entry in the segment.
    final long index = getNextIndex();

    Indexed cachedEntry = cache.get(index);
    if (cachedEntry != null) {
      this.nextEntry = cachedEntry;
      buffer.skip(memory.position() + cachedEntry.size() + Bytes.INTEGER + Bytes.INTEGER);
      memory.clear().limit(0);
      return;
    } else if (cache.index() > 0 && cache.index() < index) {
      this.nextEntry = null;
      return;
    }

    // Read more bytes from the segment if necessary.
    if (memory.remaining() < maxEntrySize) {
      buffer.skip(memory.position())
          .mark()
          .read(memory.clear().limit(maxEntrySize * 2))
          .reset();
      memory.flip();
    }

    // Mark the buffer so it can be reset if necessary.
    memory.mark();

    try {
      // Read the length of the entry.
      final int length = memory.readInt();

      // If the buffer length is zero then return.
      if (length <= 0 || length > maxEntrySize) {
        memory.reset().limit(memory.position());
        nextEntry = null;
        return;
      }

      // Read the checksum of the entry.
      long checksum = memory.readUnsignedInt();

      // Read the entry into memory.
      byte[] bytes = new byte[length];
      memory.read(bytes);

      // Compute the checksum for the entry bytes.
      final Checksum crc32 = new CRC32();
      crc32.update(bytes, 0, length);

      // If the stored checksum equals the computed checksum, return the entry.
      if (checksum == crc32.getValue()) {
        E entry = serializer.decode(bytes);
        nextEntry = new Indexed<>(index, entry, length);
      } else {
        memory.reset().limit(memory.position());
        nextEntry = null;
      }
    } catch (BufferUnderflowException e) {
      memory.reset().limit(memory.position());
      nextEntry = null;
    }
  }

  @Override
  public void close() {
    memory.close();
    buffer.close();
  }
}