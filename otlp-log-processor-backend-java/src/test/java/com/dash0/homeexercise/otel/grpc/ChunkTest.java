package com.dash0.homeexercise.otel.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ChunkTest {

	@Test
	void testChunkCreation() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
		Chunk<Integer> chunk = Chunk.ofSize(list, 3);

		assertEquals(3, chunk.size());
		assertEquals(Arrays.asList(1, 2, 3), chunk.get(0));
		assertEquals(Arrays.asList(4, 5, 6), chunk.get(1));
		assertEquals(Arrays.asList(7, 8, 9), chunk.get(2));
	}

	@Test
	void testChunkWithNonDivisibleSize() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
		Chunk<Integer> chunk = Chunk.ofSize(list, 3);

		assertEquals(3, chunk.size());
		assertEquals(Arrays.asList(1, 2, 3), chunk.get(0));
		assertEquals(Arrays.asList(4, 5, 6), chunk.get(1));
		assertEquals(List.of(7), chunk.get(2));
	}

	@Test
	void testChunkWithEmptyList() {
		List<Integer> list = Arrays.asList();
		Chunk<Integer> chunk = Chunk.ofSize(list, 3);

		assertEquals(0, chunk.size());
	}

	@Test
	void testChunkWithIndexOutOfBounds() {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
		Chunk<Integer> chunk = Chunk.ofSize(list, 2);

		assertThrows(IndexOutOfBoundsException.class, () -> chunk.get(3));
	}
}
