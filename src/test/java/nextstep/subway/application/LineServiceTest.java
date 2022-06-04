package nextstep.subway.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import nextstep.subway.domain.Line;
import nextstep.subway.domain.LineRepository;
import nextstep.subway.domain.LineStation;
import nextstep.subway.domain.Station;
import nextstep.subway.domain.StationRepository;
import nextstep.subway.dto.LineRequest;
import nextstep.subway.dto.LineResponse;
import nextstep.subway.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class LineServiceTest {
    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private LineRepository lineRepository;

    private LineService lineService;

    private Station gangnam = new Station("강남역");
    private Station yangjae = new Station("양재역");

    @BeforeEach
    void setUp() {
        lineService = new LineService(lineRepository, stationRepository);

        stationRepository.save(gangnam);
        stationRepository.save(yangjae);
    }

    @Test
    void LineRequest_객체로_노선을_생성할_수_있어야_한다() {
        // given
        final String lineName = "신분당선";
        final String color = "bg-red-600";
        final LineRequest lineRequest = new LineRequest(lineName, color, gangnam.getId(), yangjae.getId());

        // when
        final LineResponse lineResponse = lineService.saveLine(lineRequest);

        // then
        assertThat(lineResponse).isNotNull();
        assertThat(lineResponse.getId()).isGreaterThan(0L);
        assertThat(lineResponse.getName()).isEqualTo(lineName);
        assertThat(lineResponse.getStations())
                .containsExactly(StationResponse.of(gangnam), StationResponse.of(yangjae));

        final Line createdLine = lineRepository.findById(lineResponse.getId()).get();
        assertThat(createdLine.getLineStations().getPreviousOf(gangnam)).isNull();
        assertThat(createdLine.getLineStations().getPreviousOf(yangjae)).isEqualTo(gangnam);
    }

    @Test
    void LineRequest_객체의_상행종점역이나_하행종점역이_조회되지_않으면_노선_생성_시_IllegalArgumentException이_발생해야_한다() {
        // given
        final String lineName = "신분당선";
        final String color = "bg-red-600";
        final List<LineRequest> invalidRequests = Arrays.asList(
                new LineRequest(lineName, color, 0L, 0L),
                new LineRequest(lineName, color, gangnam.getId(), 0L),
                new LineRequest(lineName, color, 0L, yangjae.getId())
        );

        // when and then
        for (final LineRequest request : invalidRequests) {
            assertThatThrownBy(() -> lineService.saveLine(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void 노선_목록을_조회하면_모든_노선에_대해_LineResponse_객체_목록이_반환되어야_한다() {
        // given
        final Line line1 = givenLine();

        final Line line2 = new Line("2호선", "bg-green-600");
        line2.relateToStation(new LineStation(line2, gangnam));
        lineRepository.save(line2);

        final Line line3 = new Line("3호선", "bg-orange-600");
        line3.relateToStation(new LineStation(line3, yangjae));
        lineRepository.save(line3);

        // when
        final List<LineResponse> lines = lineService.findAllLines();

        // then
        assertThat(lines.stream().map(LineResponse::getName).collect(Collectors.toList()))
                .containsExactly(line1.getName(), line2.getName(), line3.getName());
    }

    @Test
    void 아이디로_노선을_조회하면_해당하는_노선에_대해_LineResponse_객체가_반환되어야_한다() {
        // given
        final Line givenLine = givenLine();

        // when
        final LineResponse line = lineService.findLineById(givenLine.getId());

        // then
        assertThat(line).isEqualTo(LineResponse.of(givenLine));
    }

    @Test
    void 없는_아이디로_노선_조회_시_IllegalArgumentException이_발생해야_한다() {
        // when and then
        assertThatThrownBy(() -> lineService.findLineById(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 아이디와_LineRequest_객체를_파라미터로_노선을_수정할_수_있어야_한다() {
        // given
        final Line givenLine = givenLine();

        final String newName = "수정된이름";
        final String newColor = "bg-modified-600";
        final LineRequest lineRequest = new LineRequest(newName, newColor);

        // when
        lineService.modifyLine(givenLine.getId(), lineRequest);

        // then
        assertThat(givenLine.getName()).isEqualTo(newName);
        assertThat(givenLine.getColor()).isEqualTo(newColor);
    }

    @Test
    void 없는_아이디로_노선_수정_시_IllegalArgumentException이_발생해야_한다() {
        // given
        final String newName = "수정된이름";
        final String newColor = "bg-modified-600";
        final LineRequest lineRequest = new LineRequest(newName, newColor);

        // when and then
        assertThatThrownBy(() -> lineService.modifyLine(1L, lineRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 아이디로_노선을_삭제할_수_있어야_한다() {
        // given
        final Line givenLine = givenLine();

        // when
        lineService.deleteLineById(givenLine.getId());

        // then
        assertThatThrownBy(() -> lineService.findLineById(givenLine.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 없는_아이디로_노선_삭제_시_IllegalArgumentException이_발생해야_한다() {
        // when and then
        assertThatThrownBy(() -> lineService.deleteLineById(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Line givenLine() {
        final Line givenLine = new Line("신분당선", "bg-red-600");
        givenLine.relateToStation(new LineStation(givenLine, gangnam));
        givenLine.relateToStation(new LineStation(givenLine, yangjae, gangnam));
        return lineRepository.save(givenLine);
    }
}
