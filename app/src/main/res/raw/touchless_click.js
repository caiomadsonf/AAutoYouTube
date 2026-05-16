setInterval(function() {
  var playBtn = document.querySelector('.ytp-play-button, .playing-mode .ytp-play-button');
  if (playBtn) {
    playBtn.addEventListener('touchstart', function(e) {
      e.preventDefault();
      playBtn.click();
    });
  }
}, 500);
