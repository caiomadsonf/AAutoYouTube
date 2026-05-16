setInterval(function() {
  var ads = document.querySelectorAll('.ad-showing, .ad-container, ytm-companion-ad-renderer, ytm-promoted-sparkles-web-renderer, .ytm-promoted-video-renderer');
  ads.forEach(function(ad) { ad.remove(); });
  var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button');
  if (skipBtn) skipBtn.click();
}, 300);
