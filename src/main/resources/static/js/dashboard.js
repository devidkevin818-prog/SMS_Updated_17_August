(() => {
    const data = window.dashboardData;
    if (!data || typeof Chart === 'undefined') return;
    const green = '#00A651';
    const darkGreen = '#007A3D';
    const grid = 'rgba(107,114,128,.12)';
    Chart.defaults.font.family = 'IBM Plex Sans, sans-serif';
    Chart.defaults.color = '#6B7280';
    const makeChart = (id, config) => {
        const canvas = document.getElementById(id);
        if (canvas) new Chart(canvas, config);
    };
    makeChart('employeeActivityChart', {type:'line',data:{labels:data.months,datasets:[{label:'Created',data:data.created,borderColor:green,backgroundColor:'rgba(0,166,81,.12)',fill:true,tension:.35},{label:'Updated',data:data.updated,borderColor:darkGreen,backgroundColor:'transparent',tension:.35}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{position:'bottom'}},scales:{y:{beginAtZero:true,ticks:{precision:0},grid:{color:grid}},x:{grid:{display:false}}}}});
    makeChart('signatureStatusChart', {type:'doughnut',data:{labels:data.signatureLabels,datasets:[{data:data.signatureValues,backgroundColor:[green,'#D99A2B','#DDE5E0'],borderWidth:0}]},options:{responsive:true,maintainAspectRatio:false,cutout:'70%',plugins:{legend:{position:'bottom'}}}});
    makeChart('approvalWorkloadChart', {type:'bar',data:{labels:data.approvalLabels,datasets:[{data:data.approvalValues,backgroundColor:[green,darkGreen],borderRadius:5}]},options:{indexAxis:'y',responsive:true,maintainAspectRatio:false,plugins:{legend:{display:false}},scales:{x:{beginAtZero:true,ticks:{precision:0},grid:{color:grid}},y:{grid:{display:false}}}}});
    makeChart('departmentChart', {type:'bar',data:{labels:data.departmentLabels,datasets:[{label:'Employees',data:data.departmentValues,backgroundColor:green,borderRadius:5}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:false}},scales:{y:{beginAtZero:true,ticks:{precision:0},grid:{color:grid}},x:{grid:{display:false}}}}});
})();
