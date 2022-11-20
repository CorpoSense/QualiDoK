/* Base64 string to array encoding */
function uint6ToB64(nUint6) {
  return nUint6 < 26
    ? nUint6 + 65
    : nUint6 < 52
    ? nUint6 + 71
    : nUint6 < 62
    ? nUint6 - 4
    : nUint6 === 62
    ? 43
    : nUint6 === 63
    ? 47
    : 65;
}

function base64EncArr(aBytes) {
  let nMod3 = 2;
  let sB64Enc = "";

  const nLen = aBytes.length;
  let nUint24 = 0;
  for (let nIdx = 0; nIdx < nLen; nIdx++) {
    nMod3 = nIdx % 3;
    if (nIdx > 0 && ((nIdx * 4) / 3) % 76 === 0) {
      sB64Enc += "\r\n";
    }

    nUint24 |= aBytes[nIdx] << ((16 >>> nMod3) & 24);
    if (nMod3 === 2 || aBytes.length - nIdx === 1) {
      sB64Enc += String.fromCodePoint(
        uint6ToB64((nUint24 >>> 18) & 63),
        uint6ToB64((nUint24 >>> 12) & 63),
        uint6ToB64((nUint24 >>> 6) & 63),
        uint6ToB64(nUint24 & 63)
      );
      nUint24 = 0;
    }
  }
  return (
    sB64Enc.substr(0, sB64Enc.length - 2 + nMod3) +
    (nMod3 === 2 ? "" : nMod3 === 1 ? "=" : "==")
  );
}

function strToUTF8Arr(sDOMStr) {
  let aBytes;
  let nChr;
  const nStrLen = sDOMStr.length;
  let nArrLen = 0;

  /* mapping… */
  for (let nMapIdx = 0; nMapIdx < nStrLen; nMapIdx++) {
    nChr = sDOMStr.codePointAt(nMapIdx);

    if (nChr > 65536) {
      nMapIdx++;
    }

    nArrLen +=
      nChr < 0x80
        ? 1
        : nChr < 0x800
        ? 2
        : nChr < 0x10000
        ? 3
        : nChr < 0x200000
        ? 4
        : nChr < 0x4000000
        ? 5
        : 6;
  }

  aBytes = new Uint8Array(nArrLen);

  /* transcription… */
  let nIdx = 0;
  let nChrIdx = 0;
  while (nIdx < nArrLen) {
    nChr = sDOMStr.codePointAt(nChrIdx);
    if (nChr < 128) {
      /* one byte */
      aBytes[nIdx++] = nChr;
    } else if (nChr < 0x800) {
      /* two bytes */
      aBytes[nIdx++] = 192 + (nChr >>> 6);
      aBytes[nIdx++] = 128 + (nChr & 63);
    } else if (nChr < 0x10000) {
      /* three bytes */
      aBytes[nIdx++] = 224 + (nChr >>> 12);
      aBytes[nIdx++] = 128 + ((nChr >>> 6) & 63);
      aBytes[nIdx++] = 128 + (nChr & 63);
    } else if (nChr < 0x200000) {
      /* four bytes */
      aBytes[nIdx++] = 240 + (nChr >>> 18);
      aBytes[nIdx++] = 128 + ((nChr >>> 12) & 63);
      aBytes[nIdx++] = 128 + ((nChr >>> 6) & 63);
      aBytes[nIdx++] = 128 + (nChr & 63);
      nChrIdx++;
    } else if (nChr < 0x4000000) {
      /* five bytes */
      aBytes[nIdx++] = 248 + (nChr >>> 24);
      aBytes[nIdx++] = 128 + ((nChr >>> 18) & 63);
      aBytes[nIdx++] = 128 + ((nChr >>> 12) & 63);
      aBytes[nIdx++] = 128 + ((nChr >>> 6) & 63);
      aBytes[nIdx++] = 128 + (nChr & 63);
      nChrIdx++;
    } /* if (nChr <= 0x7fffffff) */ else {
      /* six bytes */
      aBytes[nIdx++] = 252 + (nChr >>> 30);
      aBytes[nIdx++] = 128 + ((nChr >>> 24) & 63);
      aBytes[nIdx++] = 128 + ((nChr >>> 18) & 63);
      aBytes[nIdx++] = 128 + ((nChr >>> 12) & 63);
      aBytes[nIdx++] = 128 + ((nChr >>> 6) & 63);
      aBytes[nIdx++] = 128 + (nChr & 63);
      nChrIdx++;
    }
    nChrIdx++;
  }
  return aBytes;
}

$("#edit").click(function(){
    $('.summernote').summernote({
        height: 300,
        tabsize: 2,
        airMode: true,
        focus: true
    });
    $("#save").removeAttr("disabled"); // removing attribute
    $('#msg-success').addClass('hidden');
    $('#msg-failure').addClass('hidden');
    $('#msg-warning').addClass('hidden');
});

$("#save").click(function(){
     if(!$.trim($('#fileNameId').val()).length){ // zero-length string AFTER a trim
        $('#msg-warning').removeClass('hidden');
        $('#msg-success').addClass('hidden');
        $('#msg-failure').addClass('hidden');
     }else{
         $('#msg-warning').addClass('hidden');
         var encodedText = $('.summernote').code();
          $('.summernote').destroy();

          if ($('#uploadedImage').attr('src') == null) {
             var filePath = $('#inputPdfFile').attr('src');
          }else {
             var filePath = $('#uploadedImage').attr('src');
          }

          var directoryId = $('#folderId').val();
          var languageId = $('#languageId').val();
          var fileNameId = $('#fileNameId').val();
         var aMyUTF8Input = strToUTF8Arr(encodedText);
         var encodedPayload = base64EncArr(aMyUTF8Input);

         var loading = $('#loading');
         loading.show();

       $.ajax({
             url: 'save',
             type: 'POST',
             contentType: 'application/json',
             data: JSON.stringify({'payload': encodedPayload ,
                                   'inputFile': filePath ,
                                   'directoryId': directoryId ,
                                   'languageId': languageId ,
                                   'fileNameId': fileNameId}),
             success: function (data) {
                 console.log(data);
                 loading.hide();
                 $('#msg-success').removeClass('hidden');
             },
             error: function(jqXHR, errMsg) {
                 console.log(errMsg);
                 loading.hide();
                 $('#msg-failure').removeClass('hidden');
             },
             always: function(jqXHR, status){
                 $("#save").removeAttr('disabled');
             }
       });
     }
});

$("#upload").click(function(){
      if(!$.trim($('#fileNameId').val()).length){ // zero-length string AFTER a trim
          $('#msg-warning').removeClass('hidden');
          $('#msg-success').addClass('hidden');
          $('#msg-failure').addClass('hidden');
      }else{
            var outputFile = $('#outputFile').attr('src');
            var directoryId = $('#folderId').val();
            var languageId = $('#languageId').val();
            var fileNameId = $('#fileNameId').val();
            var loading = $('#loading');
            loading.show();
            $('#msg-warning').removeClass('hidden');
            $('#msg-success').addClass('hidden');
            $('#msg-failure').addClass('hidden');
            $.ajax({
                url: 'uploadDoc',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({'directoryId': directoryId ,
                                      'languageId': languageId,
                                      'outputFile':outputFile,
                                      'fileNameId':fileNameId }),
                success: function (data) {
                    console.log(data);
                    $('#msg-success').removeClass('hidden');
                    $('#msg-warning').addClass('hidden');
                    $("#upload").prop("disabled", true);
                },
                error: function(jqXHR, errMsg) {
                    console.log(errMsg);
                    loading.hide();
                    $('#msg-failure').removeClass('hidden');
                    $('#msg-warning').addClass('hidden');
                    $("#upload").prop("disabled", true);
                }
            });
      }

});