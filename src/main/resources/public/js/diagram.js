const mac = document.getElementById("mac").innerHTML,
    // d3
    formatTime = d3.timeFormat("%w, %H:%M");


function maketable(input_svg, input_data, text_title, text_data, max_value){
    const svg = d3.select(input_svg);
    //const svg = d3.select('#container');
    const margin = 80;
    const width = 1000 - 2 * margin;
    const height = 600 - 2 * margin;

    const chart = svg.append('g')
        .attr('transform', `translate(${margin}, ${margin})`);

    const xScale = d3.scaleBand()
        .range([0, width])
        .domain(input_data.map(function(s){
            return formatTime(new Date(s.time));
        }))
        .padding(0.4);

    const yScale = d3.scaleLinear()
        .range([height, 0])
        .domain([0, max_value]);

    // vertical grid lines
    // const makeXLines = () => d3.axisBottom()
    //   .scale(xScale)

    const makeYLines = () => d3.axisLeft()
        .scale(yScale);


    chart.append('g')
        .attr('transform', `translate(0,${height})`)
        .call(d3.axisBottom(xScale))
        .selectAll("text")
        .attr("y", 0)
        .attr("x", 9)
        .attr("dy", ".35em")
        .attr("transform", "rotate(90)")
        .style("text-anchor", "start");

    chart.append('g')
        .call(d3.axisLeft(yScale));

    // vertical grid lines
    // chart.append('g')
    //   .attr('class', 'grid')
    //   .attr('transform', `translate(0, ${height})`)
    //   .call(makeXLines()
    //     .tickSize(-height, 0, 0)
    //     .tickFormat('')
    //   )

    chart.append('g')
        .attr('class', 'grid')
        .call(makeYLines()
            .tickSize(-width, 0, 0)
            .tickFormat('')
        );

    const barGroups = chart.selectAll()
        .data(input_data)
        .enter()
        .append('g');

    barGroups
        .append('rect')
        .attr('class', 'bar')
        .attr('x', (g) => xScale(formatTime(new Date(g.time))))
        .attr('y', (g) => yScale(g.data))
        .attr('height', (g) => height - yScale(g.data))
        .attr('width', xScale.bandwidth())
        .on('mouseenter', function (actual, i) {
            d3.selectAll(input_data.data)
                .attr('opacity', 0);

            d3.select(this)
                .transition()
                .duration(300)
                .attr('opacity', 0.8)
                .attr('x', (a) => xScale(formatTime(new Date(a.time))))
                .attr('width', xScale.bandwidth() + 10);

            const y = yScale(actual.data);

            line = chart.append('line')
                .attr('id', 'limit')
                .attr('x1', 0)
                .attr('y1', y)
                .attr('x2', width)
                .attr('y2', y);

            barGroups.append('text')
                .attr('class', 'divergence')
                .attr('x', (a) => xScale(formatTime(new Date(a.time))) + xScale.bandwidth() / 2)
                .attr('y', (a) => yScale(a.data) - 20)
                .attr('fill', 'white')
                .attr('text-anchor', 'middle')
                .text((a, idx) => {
                const divergence = (a.data - actual.data).toFixed(1)
                    let text = '';
                    if (divergence > 0)
                        text += '+'
                    text += `${divergence}`

                    return idx !== i ? text : '';
                });

        })
        .on('mouseleave', function () {
            d3.selectAll('.data')
                .attr('opacity', 1)

            d3.select(this)
                .transition()
                .duration(300)
                .attr('opacity', 1)
                .attr('x', (a) => xScale(formatTime(new Date(a.time))))
                .attr('width', xScale.bandwidth())

            chart.selectAll('#limit').remove()
            chart.selectAll('.divergence').remove()
        });

    barGroups
        .append('text')
        .attr('class', 'value')
        .attr('x', (a) => xScale(formatTime(new Date(a.time))) + xScale.bandwidth() / 2)
        .attr('y', (a) => yScale(a.data) - 3)
        .attr('text-anchor', 'middle')
        .text((a) => `${a.data}`);

    svg.append('text')
        .attr('class', 'label')
        .attr('x', -(height / 2) - margin)
        .attr('y', margin / 2.4)
        .attr('transform', 'rotate(-90)')
        .attr('text-anchor', 'middle')
        .text(text_data);

    svg.append('text')
        .attr('class', 'label')
        .attr('x', width / 2 + margin)
        .attr('y', height + margin * 1.7)
        .attr('text-anchor', 'middle')
        .text(`End of Period`);

    svg.append('text')
        .attr('class', 'title')
        .attr('x', width / 2 + margin)
        .attr('y', 40)
        .attr('text-anchor', 'middle')
        .text(text_title);

    svg.append('text')
        .attr('class', 'source')
        .attr('x', width - margin / 2)
        .attr('y', height + margin * 1.7)
        .attr('text-anchor', 'start')
        .text('Spyke: '+new Date().toDateString());
}
$.get(`/device/control/period/${mac}/limited/`,
    function(result){
        // console.log(result);
        // Server response has 7 days data, pick today
        const passed_data = result.reduce(function(array, item){
            const newItem = {
                data: item.passedBytes,
                time: item.id.endTime,
            };
            array.push(newItem);
            return array;
        }, []);
        // console.log(passed_data);

        const sum_passed_bytes = passed_data.map(function(obj){return obj.data;}).reduce(function (a, b){return a+b;}, 0);

        const max_passed_bytes = passed_data.reduce(function(max, p) {return (p.data > max ? p.data : max);}, 0);

        const min_passed_bytes = passed_data.reduce(function(min, p) {
            if(p.data !== 0)
                return p.data < min ? p.data : min;
            return min;
            }, 0);

        let passed_unit = 'Bytes';
        let passed_totalUnit = 'KiloBytes';
        let passed_divisor = 1;

        if (min_passed_bytes > 1024) {
            passed_unit = 'KiloBytes';
            passed_totalUnit = 'MegaBytes';
            passed_divisor = 1024;
        } else if (min_passed_bytes > (1024*1024)) {
            passed_unit = 'MegaBytes';
            passed_totalUnit = 'GigaBytes';
            passed_divisor = 1024*1024;
        } else if (min_passed_bytes > (1024*1024*1024)) {
            passed_unit = 'GigaBytes';
            passed_totalUnit = 'TeraBytes';
            passed_divisor = 1024*1024*1024;
        }

        maketable(
            '#pass_svg',
            passed_data,
            `Total ${sum_passed_bytes/passed_divisor} ${passed_unit} [${Math.floor(sum_passed_bytes/(passed_divisor*1024))} ${passed_totalUnit}] passed`,
            `Data [${passed_unit}]`,
            max_passed_bytes
        );

        // Server response has 7 days data, group them by Date.getDay()
        const dropped_data = result.reduce(function(array, item){
            const newItem = {
                data: item.droppedBytes,
                time: item.id.endTime,
            }
            array.push(newItem);
            return array;
        }, []);
        console.log(dropped_data);

        const sum_dropped_bytes = dropped_data.map(function(obj){return obj.data}).reduce(function (a, b){return a+b;}, 0);

        const max_dropped_bytes = dropped_data.reduce(function(max, p) {return (p.data > max ? p.data : max);}, 0);

        const min_dropped_bytes = dropped_data.reduce(function(min, p) {
            if(p.data !== 0)
                return p.data < min ? p.data : min;
            return min;
            }, 0);

        let dropped_unit = 'Bytes';
        let dropped_totalUnit = 'KiloBytes';
        let dropped_divisor = 1;

        if (min_dropped_bytes > 1024) {
            dropped_unit = 'KiloBytes';
            dropped_totalUnit = 'MegaBytes';
            dropped_divisor = 1024;
        } else if (min_dropped_bytes > (1024*1024)) {
            dropped_unit = 'MegaBytes';
            dropped_totalUnit = 'GigaBytes';
            dropped_divisor = 1024*1024;
        } else if (min_dropped_bytes > (1024*1024*1024)) {
            dropped_unit = 'GigaBytes';
            dropped_totalUnit = 'TeraBytes';
            dropped_divisor = 1024*1024*1024;
        }

        maketable(
            '#drop_svg',
            dropped_data,
            `Total ${sum_dropped_bytes/dropped_divisor} ${dropped_unit} [${Math.floor(sum_dropped_bytes/(dropped_divisor*1024))} ${dropped_totalUnit}] dropped`,
            `Data [${dropped_unit}]`,
            max_dropped_bytes
        );

    });

