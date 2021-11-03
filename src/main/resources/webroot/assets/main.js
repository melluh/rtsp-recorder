const cameraSelect = document.getElementById("camera-select");
const dateSelect = document.getElementById("date-select");
const recordingsContainer = document.getElementById("recordings");
const videoView = document.getElementById("video-view");
const nowViewingElement = document.getElementById("now-viewing");

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
    recordingsContainer.innerHTML = "";

    fetch("/api/recordings?camera=" + cameraSelect.value + "&date=" + formatDate(dateSelect.valueAsDate)).then(r => r.json()).then(json => {
        json.forEach(recording => {
            var recordingElement = document.createElement("div");
            recordingElement.classList.add("recording");
            recordingsContainer.appendChild(recordingElement);

            var durationElement = document.createElement("p");
            durationElement.innerHTML = recording.startTime + " - " + recording.endTime;
            durationElement.classList.add("recording-duration");
            recordingElement.appendChild(durationElement);

            var playIcon = document.createElement("img");
            playIcon.src = "assets/icons/eye.svg";
            playIcon.classList.add("recording-view-icon")
            recordingElement.appendChild(playIcon);

            playIcon.addEventListener("click", e => {
                videoView.src = "/recordings/" + recording.filePath;
                videoView.play();
                nowViewingElement.innerHTML = "Now viewing: <a href=\"/recordings/" + recording.filePath + "?download=1\">" + recording.filePath + "</a>";

                Array.from(document.querySelectorAll(".recording-view-icon-active")).forEach((el) => el.classList.remove("recording-view-icon-active"));
                playIcon.classList.add("recording-view-icon-active");
            });
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
