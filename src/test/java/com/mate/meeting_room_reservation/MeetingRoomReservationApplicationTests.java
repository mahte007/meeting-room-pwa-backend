package com.mate.meeting_room_reservation;

import com.mate.meeting_room_reservation.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MeetingRoomReservationApplicationTests {

	@Test
	void contextLoads() {
	}

}
