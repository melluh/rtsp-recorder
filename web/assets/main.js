const cameraSelect = document.getElementById("camera-select");
const dateSelect = document.getElementById("date-select");
const recordingsTable = document.getElementById("recordings-table");

// Set default and max value to today
dateSelect.valueAsDate = new Date();
dateSelect.max = formatDate(new Date());

// When the selected camera or date changes, re-load recordings
cameraSelect.addEventListener("change", () => loadRecordings());
dateSelect.addEventListener("change", () => loadRecordings());

var cameras = [];

function loadStatus() {
    fetch("/api/status").then(r => r.json()).then(json => {
        cameras = Object.keys(json.cameras);
        updateCamerasDropdown();
    });
}
loadStatus();

function updateCamerasDropdown() {
    cameraSelect.innerHTML = "";

    var optHeader = document.createElement("option");
    cameraSelect.appendChild(optHeader);
    optHeader.innerHTML = "Select a camera";
    optHeader.disabled = true;

    cameras.forEach(camera => {
        var opt = document.createElement("option");
        cameraSelect.appendChild(opt);
        opt.innerHTML = camera;
    });
}

function loadRecordings() {
    recordingsTable.innerHTML = "";

    fetch("/api/recordings?camera=" + cameraSelect.value + "&date=" + formatDate(dateSelect.valueAsDate)).then(r => r.json()).then(json => {
        json.forEach(recording => {
            var row = recordingsTable.insertRow();
            row.insertCell().innerHTML = "<a href=\"/recordings/" + recording.filePath + "\">" + recording.filePath + "</a>";
            row.insertCell().innerHTML =  recording.startTime + " - " + recording.endTime;
        });
    });
}

function formatDate(date) {
    return date.toLocaleDateString("en-CA"); // yyyy-mm-dd
}

function pad(num, size) {
    num = num.toString();
    while(num.length < size) num = "0" + num;
    return num;
}
